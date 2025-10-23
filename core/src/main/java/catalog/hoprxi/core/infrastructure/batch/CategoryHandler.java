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
import catalog.hoprxi.core.application.view.CategoryView;
import catalog.hoprxi.core.domain.model.Name;
import catalog.hoprxi.core.domain.model.category.Category;
import catalog.hoprxi.core.domain.model.category.CategoryRepository;
import catalog.hoprxi.core.infrastructure.PsqlUtil;
import catalog.hoprxi.core.infrastructure.i18n.Label;
import catalog.hoprxi.core.infrastructure.persistence.postgresql.PsqlCategoryRepository;
import catalog.hoprxi.core.infrastructure.query.postgresql.PsqlCategoryQuery;
import com.lmax.disruptor.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.regex.Pattern;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuan</a>
 * @since JDK8.0
 * @version 0.0.1 builder 2023-05-08
 */
public class CategoryHandler implements EventHandler<ItemImportEvent> {
    private static final Pattern ID_PATTERN = Pattern.compile("^\\d{12,19}$");
    private static long ROOT_ID;
    private static final Logger LOGGER = LoggerFactory.getLogger(CategoryHandler.class);
    private final PsqlCategoryQuery CATEGORY_QUERY = new PsqlCategoryQuery();
    private final CategoryRepository repository = new PsqlCategoryRepository();

    public CategoryHandler() {
        CategoryView[] root = CATEGORY_QUERY.root();
        if (root.length == 0)
            repository.save(Category.UNDEFINED);
        Name rootName = new Name("商品分类", "root");
        for (CategoryView v : root) {
            if (v.getName().equals(rootName)) {
                ROOT_ID = v.getId();
                break;
            }
        }
        if (ROOT_ID == 0) {
            long rootId = repository.nextIdentity();
            Category root1 = Category.root(rootId, rootName);
            repository.save(root1);
            ROOT_ID = rootId;
        }
    }

    @Override
    public void onEvent(ItemImportEvent itemImportEvent, long l, boolean b) throws Exception {
        String category = itemImportEvent.map.get(ItemMapping.CATEGORY);
        if (category == null || category.isBlank() || category.equalsIgnoreCase("undefined") || category.equalsIgnoreCase(Label.CATEGORY_UNDEFINED)) {
            itemImportEvent.map.put(ItemMapping.CATEGORY, String.valueOf(Category.UNDEFINED.id()));
            return;
        }
        if (ID_PATTERN.matcher(category).matches()) {//valid category id
            if (!this.find(Long.parseLong(category)))//没有查到该id
                itemImportEvent.map.put(ItemMapping.CATEGORY, String.valueOf(Category.UNDEFINED.id()));
            return;
        }
        String[] ss = category.split("/");
        CategoryView[] categoryViews = CATEGORY_QUERY.queryByName("^" + ss[ss.length - 1] + "$");
        if (categoryViews.length >= 1) {
            itemImportEvent.map.put(ItemMapping.CATEGORY, String.valueOf(categoryViews[0].getId()));
        } else {
            long parentId = ROOT_ID;
            Category temp;
            for (String s : ss) {
                categoryViews = CATEGORY_QUERY.queryByName("^" + s + "$");
                if (categoryViews.length == 0) {
                    temp = new Category(parentId, repository.nextIdentity(), s);
                    repository.save(temp);
                    //System.out.println("新建：" + temp);
                    parentId = temp.id();
                } else {
                    parentId = categoryViews[0].getId();
                }
            }
            //System.out.println("正确的类别id：" + parentId);
            itemImportEvent.map.put(ItemMapping.CATEGORY, String.valueOf(parentId));
        }
        //System.out.println("category:" +itemImportEvent.map.get(Corresponding.CATEGORY));
    }

    private boolean find(long id) {
        final String query = "select id from category where id = ?";
        try (Connection connection = PsqlUtil.getConnection()) {
            PreparedStatement preparedStatement = connection.prepareStatement(query);
            preparedStatement.setLong(1, id);
            ResultSet rs = preparedStatement.executeQuery();
            if (rs.next())
                return true;
        } catch (SQLException e) {
            LOGGER.error("e: ", e);
            //LOGGER.error("Can't rebuild brand with (name = {})", name, e);
        }
        return false;
    }
}
