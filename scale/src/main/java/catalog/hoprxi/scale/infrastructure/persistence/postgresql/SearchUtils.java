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

package catalog.hoprxi.scale.infrastructure.persistence.postgresql;


import catalog.hoprxi.core.domain.model.Name;
import catalog.hoprxi.core.domain.model.Specification;
import catalog.hoprxi.core.domain.model.madeIn.MadeIn;
import com.huaban.analysis.jieba.JiebaSegmenter;
import com.huaban.analysis.jieba.SegToken;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK21
 * @version 0.0.1 builder 2026/3/13
 */

public final class SearchUtils {
    private static final JiebaSegmenter segmenter = new JiebaSegmenter();

    // 定义一个“有意义的单字白名单” (根据业务扩展)
    // 这里只列举了部分，实际需要根据你的行业词库补充
    private static final Set<String> MEANINGFUL_SINGLE_CHARS = new HashSet<>(Arrays.asList(
            "鱼", "肉", "菜", "果", "奶", "蛋", "药", "茶", "酒", "糖",
            "车", "房", "衣", "鞋", "包", "机", "电", "灯", "纸", "笔",
            "猪", "牛", "羊", "鸡", "鸭", "鹅", "虾", "蟹", "贝"
            // ... 继续添加你业务中用户可能会单独搜的单字
    ));

    // 定义停用词表 (长度=1 且无意义的字)
    private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(
            "的", "了", "是", "在", "我", "有", "和", "就", "不", "人",
            "都", "一", "一个", "上", "也", "很", "到", "说", "要", "去",
            "你", "会", "着", "没有", "看", "好", "自己", "这"
            // ... 可以引入现成的中文停用词表
    ));

    /**
     * 将多个字段组合并分词，生成 search_vector 内容
     */
    public static String buildSearchVector(Name name, Specification spec, MadeIn madeIn) {
        StringBuilder rawText = new StringBuilder();

        // 1. 拼接所有需要被搜索的字段
        if (name != null)
            rawText.append(" ").append(name.name()).append(" ").append(name.alias()).append(" ").append(name.mnemonic());
        if (spec != null) rawText.append(" ").append(spec.value());
        if (madeIn != null) rawText.append(" ").append(madeIn.madeIn());

        // 2. 使用 Jieba 分词
        // 使用 index 模式：适合搜索引擎，会切分出更细的粒度（如 "红富士" -> "红", "富士", "红富士"）
        List<SegToken> tokens = segmenter.process(rawText.toString(), JiebaSegmenter.SegMode.INDEX);

        // 3. 过滤掉单字（可选，视需求而定，通常单字噪音大）并去重
        return tokens.stream()
                .map(t -> t.word)
                .filter(w -> {
                    if (w.length() > 1) {
                        return true; // 多字词全部保留
                    } else {
                        // 单字词：如果在白名单里，保留；如果在停用词里，丢弃
                        return MEANINGFUL_SINGLE_CHARS.contains(w) && !STOP_WORDS.contains(w);
                    }
                }) // 过滤掉单字，提高准确率，如需搜单字可去掉此行
                .distinct()
                .collect(Collectors.joining(" "));
    }
}
