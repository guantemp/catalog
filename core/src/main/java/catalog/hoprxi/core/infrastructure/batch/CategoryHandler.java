/*
 * Copyright (c) 2026. www.hoprxi.com All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package catalog.hoprxi.core.infrastructure.batch;

import catalog.hoprxi.core.application.batch.ItemMapping;
import catalog.hoprxi.core.domain.model.Name;
import catalog.hoprxi.core.domain.model.category.Category;
import catalog.hoprxi.core.domain.model.category.CategoryRepository;
import catalog.hoprxi.core.infrastructure.PsqlUtil;
import catalog.hoprxi.core.infrastructure.i18n.Label;
import catalog.hoprxi.core.infrastructure.persistence.postgresql.PsqlCategoryRepository;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.WorkHandler;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuan</a>
 * @since JDK21
 * @version 0.0.1 builder 2026-07-12
 */
public class CategoryHandler implements EventHandler<ItemImportEvent>, WorkHandler<ItemImportEvent> {
    private static final Pattern ID_PATTERN = Pattern.compile("^-?\\d{1,19}$");
    private static final long FAMILY_ID;
    private static final String NAME;
    private static final String SHORTNAME;
    private static final String DELIMITER;
    private static final Logger LOGGER = LoggerFactory.getLogger(CategoryHandler.class);
    private static final CategoryRepository REPOSITORY = new PsqlCategoryRepository();
    private static final Map<String, Long> CATEGORY_CACHE = new ConcurrentHashMap<>(1024);

    static {
        Config config = ConfigFactory.load("import");
        config = config.getConfig("category");
        DELIMITER = config.hasPath("delimiter") ? config.getString("delimiter") : "/";
        NAME = config.hasPath("root_name") ? config.getString("root_name") : "商品分类";
        SHORTNAME = config.hasPath("root_shortName") ? config.getString("root_shortName") : "root of catalog category";
        Category[] roots = CategoryHandler.root();
        long id = 0L;
        for (Category v : roots) {
            Name temp = v.name();
            if (NAME.equals(temp.name()) || SHORTNAME.equals(temp.shortName())) {
                id = v.id();
                break;
            }
        }
        if (id == 0L) {
            id = REPOSITORY.nextIdentity();
            Category root = Category.root(id, new Name(NAME, SHORTNAME));
            //System.out.println(root);
            REPOSITORY.save(root);
        }
        FAMILY_ID = id;
    }

    @Override
    public void onEvent(ItemImportEvent event) throws Exception {
        this.onEvent(event, 0, false);
    }


    @Override
    public void onEvent(ItemImportEvent event, long l, boolean b) throws Exception {
        String category = event.map.get(ItemMapping.CATEGORY);
        if (category == null || category.isBlank()
            || category.equalsIgnoreCase(Category.UNCATEGORIZED.name().name())
            || category.equalsIgnoreCase(Category.UNCATEGORIZED.name().shortName())
            || category.equalsIgnoreCase(Label.UNCATEGORIZED)) {
            event.map.put(ItemMapping.CATEGORY, String.valueOf(Category.UNCATEGORIZED.id()));
            return;
        }
        if (ID_PATTERN.matcher(category).matches()) {//valid category id
            if (Category.UNCATEGORIZED.id() == Long.parseLong(category))//UNCATEGORIZED
                return;
            if (!CategoryHandler.isdExists(Long.parseLong(category)))//是id值的形式，但是数据库不存在该id
                event.map.put(ItemMapping.CATEGORY, String.valueOf(Category.UNCATEGORIZED.id()));
            return;
        }

        // ---------- 处理路径形式，例如 "电子/手机/苹果" ----------
        String[] nodes = category.split(DELIMITER);
        if (nodes.length == 0) {
            event.map.put(ItemMapping.CATEGORY, String.valueOf(Category.UNCATEGORIZED.id()));
            return;
        }
        //System.out.println(Arrays.toString(nodes));
        long parentId = FAMILY_ID; // 从根开始
        int i = 0;
        // 逐级向下查找，直到第一个不存在的节点
        for (; i < nodes.length; i++) {
            String nodeName = nodes[i];
            long id = CategoryHandler.findIdByParentAndName(parentId, nodeName);
            if (id == Long.MIN_VALUE) {
                break; // 该节点缺失，从这一级开始创建
            }
            parentId = id; // 继续向下
        }

        // 如果缺失的节点名称为空白，直接归为 UNCATEGORIZED，不创建
        if (i < nodes.length) {
            String firstMissing = nodes[i];
            if (firstMissing == null || firstMissing.isBlank()) {
                event.map.put(ItemMapping.CATEGORY, String.valueOf(Category.UNCATEGORIZED.id()));
                return;
            }
        }

        // 从缺失的节点开始，依次创建
        for (int j = i; j < nodes.length; j++) {
            String nodeName = nodes[j];
            //System.out.println("create:"+nodeName);
            long finalParentId = parentId;
            String cacheKey = finalParentId + "_" + nodeName;
            // 使用 computeIfAbsent 保证多线程下只创建一次
            parentId = CATEGORY_CACHE.computeIfAbsent(cacheKey, k -> {
                long newId = REPOSITORY.nextIdentity();
                Category temp = new Category(finalParentId, newId, nodeName);
                System.out.println(temp);
                REPOSITORY.save(temp);
                return newId;
            });
        }

        // 最终 parentId 就是最后一级（叶子节点）的 ID
        event.map.put(ItemMapping.CATEGORY, String.valueOf(parentId));
        //System.out.println("category:" +itemImportEvent.map.get(Corresponding.CATEGORY));
    }

    private static long findIdByParentAndName(long parentId, String nodeName) {
        String cacheKey = parentId + "_" + nodeName;
        Long cached = CATEGORY_CACHE.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        String query = """
                SELECT id FROM category
                WHERE parent_id = ?
                 AND (name::jsonb->>'name' = ?
                 OR (LENGTH(?) > 0 AND name::jsonb->>'shortName' = ?))
                LIMIT 1
                """;
        try (Connection conn = PsqlUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setLong(1, parentId);
            ps.setString(2, nodeName);
            ps.setString(3, nodeName);
            ps.setString(4, nodeName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    long id = rs.getLong(1);
                    CATEGORY_CACHE.put(cacheKey, id);
                    return id;
                }
            }
        } catch (SQLException e) {
            LOGGER.error("Failed to find category by parentId={}, nodeName={}", parentId, nodeName, e);
        }
        return Long.MIN_VALUE;
    }

    private static boolean isdExists(long id) {
        if (id == Category.UNCATEGORIZED.id()) return true;
        String query = "SELECT 1 FROM category WHERE id = ?";
        try (Connection conn = PsqlUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            LOGGER.error("Failed to check existence of category id={}", id, e);
            return false;
        }
    }

    private static Category[] root() {
        List<Category> categoryList = new ArrayList<>();
        final String rootSql = "select id,name::jsonb->>'name' as name,name::jsonb->>'shortName' as shortName from category where id = parent_id";
        try (Connection connection = PsqlUtil.getConnection(); PreparedStatement preparedStatement = connection.prepareStatement(rootSql)) {
            try (ResultSet rs = preparedStatement.executeQuery()) {
                while (rs.next()) {
                    categoryList.add(new Category(rs.getLong(1), rs.getLong(1), new Name(rs.getString(2), rs.getString(3))));
                }
            }
        } catch (SQLException e) {
            LOGGER.error("Can't rebuild category", e);
            throw new RuntimeException(e);
        }
        return categoryList.toArray(new Category[0]);
    }
}
