/*
 * Copyright (c) 2025. www.hoprxi.com All Rights Reserved.
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
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuan</a>
 * @since JDK8.0
 * @version 0.0.1 builder 2023-05-08
 */
public class CategoryHandler implements EventHandler<ItemImportEvent> {
    private static final Pattern ID_PATTERN = Pattern.compile("^\\d{1,19}$");
    private static volatile long family_id = 0L;
    private static final String name;
    private static final String shortName;
    private static final String DELIMITER;
    private static final Logger LOGGER = LoggerFactory.getLogger(CategoryHandler.class);
    private final CategoryRepository repository = new PsqlCategoryRepository();
    private static final Map<String, Long> CATEGORY_CACHE = new ConcurrentHashMap<>(1024);

    static {
        Config config = ConfigFactory.load("import");
        config = config.getConfig("category");
        DELIMITER = config.hasPath("delimiter") ? config.getString("delimiter") : "/";
        name = config.hasPath("name") ? config.getString("name") : "商品分类";
        shortName = config.hasPath("shortName") ? config.getString("shortName") : "root of catalog category";
    }

    public CategoryHandler() {
        Category[] roots = CategoryHandler.root();
        if (roots.length == 0)
            repository.save(Category.UNCATEGORIZED);
        //根据分类名称-》找到 family_id
        for (Category v : roots) {
            Name temp = v.name();
            if (name.equals(temp.name()) || shortName.equals(temp.shortName())) {
                family_id = v.id();
                break;
            }
        }
        //还没有根，需要建一个
        if (family_id == 0L) {
            long rootId = repository.nextIdentity();
            Category root = Category.root(rootId, new Name(name, shortName));
            repository.save(root);
            family_id = rootId;
        }
    }

    @Override
    public void onEvent(ItemImportEvent itemImportEvent, long l, boolean b) throws Exception {
        String category = itemImportEvent.map.get(ItemMapping.CATEGORY);
        if (category == null || category.isBlank()
            || category.equalsIgnoreCase(Category.UNCATEGORIZED.name().name())
            || category.equalsIgnoreCase(Category.UNCATEGORIZED.name().shortName())
            || category.equalsIgnoreCase(Label.UNCATEGORIZED)) {
            itemImportEvent.map.put(ItemMapping.CATEGORY, String.valueOf(Category.UNCATEGORIZED.id()));
            return;
        }
        if (ID_PATTERN.matcher(category).matches()) {//valid category id
            if (Category.UNCATEGORIZED.id() == Long.parseLong(category))//UNDEFINED
                return;
            if (!CategoryHandler.find(Long.parseLong(category)))//是id值的形式，但是数据库不存在该id
                itemImportEvent.map.put(ItemMapping.CATEGORY, String.valueOf(Category.UNCATEGORIZED.id()));
            return;
        }

        String[] ss = category.split(DELIMITER);
        long parentId = family_id;
        int position = 0;
        for (int i = ss.length - 1; i >= 0; i--) {
            long id = CategoryHandler.findIdByName(ss[i]);
            //最后一个在数据库找到了，a/b/c/d即d找到id了
            if (id != Category.UNCATEGORIZED.id() && i == ss.length - 1) {
                itemImportEvent.map.put(ItemMapping.CATEGORY, String.valueOf(id));
                return;
            }
            //向上循环查找，找到就终止，直到最顶层那个
            position = i;
            if (id != Category.UNCATEGORIZED.id()) {
                parentId = id;
                break;
            }
        }
        for (int i = position + 1, j = ss.length; i < j; i++) {
            String nodeName = ss[i];

            // 1. 创建前再次检查缓存，防止多线程并发创建同名父节点
            Long cachedId = CATEGORY_CACHE.get(nodeName);
            if (cachedId != null) {
                parentId = cachedId;
                continue;
            }
            Category temp = new Category(parentId, repository.nextIdentity(), nodeName);
            repository.save(temp);
            CATEGORY_CACHE.put(ss[i], temp.id());
            parentId = temp.id();
        }
        //最后一个parentId就是自己的id,不再循环了
        itemImportEvent.map.put(ItemMapping.CATEGORY, String.valueOf(parentId));
        //System.out.println("category:" +itemImportEvent.map.get(Corresponding.CATEGORY));
    }

    private static long findIdByName(String nodeName) {
        Long cachedId = CATEGORY_CACHE.get(nodeName);
        if (cachedId != null) {
            return cachedId;
        }
        final String query = """
                SELECT id FROM category
                WHERE family_id = ?
                 AND (name::jsonb->>'name' = ? OR name::jsonb->>'shortName' = ?)
                ORDER BY (name::jsonb->>'name' = ?) DESC
                LIMIT 1"""; // 找到第一个匹配项立即返回，减少扫描
        try (Connection connection = PsqlUtil.getConnection(); PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setLong(1, family_id);
            ps.setString(2, nodeName);
            ps.setString(3, nodeName);
            ps.setString(4, nodeName);     // 参数 4 (对应 ORDER BY)
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    long id = rs.getLong("id");
                    CATEGORY_CACHE.put(nodeName, id);
                    return id;
                }
            }
        } catch (SQLException e) {
            LOGGER.error("e: ", e);
            //LOGGER.error("Can't rebuild brand with (name = {})", name, e);
        }
        return Category.UNCATEGORIZED.id();
    }

    private static boolean find(long id) {
        if (id == Category.UNCATEGORIZED.id())
            return true;
        final String query = "select id from category where id = ?";
        try (Connection connection = PsqlUtil.getConnection(); PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setLong(1, id);
            try (ResultSet rs = preparedStatement.executeQuery()) {
                if (rs.next())
                    return true;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return false;
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
