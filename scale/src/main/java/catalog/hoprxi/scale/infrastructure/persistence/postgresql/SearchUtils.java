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
import salt.hoprxi.to.PinYin;

import java.util.*;
import java.util.stream.Collectors;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK21
 * @version 0.0.1 builder 2026/3/13
 */

public final class SearchUtils {
    private static final JiebaSegmenter segmenter = new JiebaSegmenter();

    // 定义一个“有意义的单字白名单” (根据业务扩展)
    private static final Set<String> MEANINGFUL_SINGLE_CHARS = new HashSet<>(Arrays.asList(
            // --- 生鲜蔬果 ---
            "菜", "果", "瓜", "豆", "笋", "菇", "菌", "葱", "姜", "蒜", "椒", "茄", "藕", "薯", "芋",
            "桃", "李", "杏", "梨", "枣", "柿", "莓", "柑", "橘", "橙", "柚", "蕉", "葡", "芒", "荔",

            // --- 肉禽蛋水产 ---
            "肉", "猪", "牛", "羊", "鸡", "鸭", "鹅", "鸽", "兔", "蛋", "禽",
            "鱼", "虾", "蟹", "贝", "螺", "蛤", "蛏", "蚝", "鲍", "参", "翅", "肚", "丸", "滑",

            // --- 米面粮油/干货 ---
            "米", "面", "粉", "条", "丝", "皮", "饺", "饨", "包", "饼", "糕", "粽", "糍", "团",
            "油", "盐", "酱", "醋", "糖", "蜜", "茶", "酒", "烟", "奶", "酪", "酥", "油", "脂",
            "粮", "谷", "麦", "稻", "粟", "黍", "豆", "腐", "竹", "笋", "耳", "菇", "菜", "干",

            // --- 休闲零食/饮料 ---
            "糖", "果", "脯", "饯", "糕", "点", "心", "派", "卷", "奶", "茶", "水", "露", "汁", "浆",

            // --- 日化/百货 (常作为商品类别简称) ---
            "皂", "粉", "液", "膏", "霜", "乳", "水", "露", "油", "精", "盐", "刷", "巾", "纸",
            "盆", "桶", "杯", "碗", "盘", "碟", "勺", "筷", "刀", "叉", "锅", "壶", "瓶", "罐",
            "巾", "被", "套", "枕", "席", "垫", "毯", "袜", "鞋", "帽", "衣", "裤", "裙", "衫"
    ));

    // 定义停用词表 (长度=1 且无意义的字)
    private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(
            "的", "了", "是", "在", "我", "有", "和", "就", "不", "人",
            "都", "一", "一个", "上", "也", "很", "到", "说", "要", "去",
            "你", "会", "着", "没有", "看", "好", "自己", "这"
            // ... 可以引入现成的中文停用词表
    ));

    // 不可拆分的双字词 (连绵词、音译词等，拆了就没意义了)
    private static final Set<String> UNBREAKABLE_DOUBLE_CHARS = new HashSet<>(Arrays.asList(
            "葡萄", "琵琶", "咖啡", "沙发", "巧克力", "奥林匹克",
            "中国", "美国", "日本", "北京", "上海"
            // ... 补充专有名词
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
                .flatMap(w -> {
                    List<String> result = new ArrayList<>();
                    // 1. 原词永远加入
                    result.add(w);
                    // 2. 如果是双字 且 不在“不可拆分列表”中，尝试拆分
                    if (w.length() == 2 && !UNBREAKABLE_DOUBLE_CHARS.contains(w)) {
                        String s1 = String.valueOf(w.charAt(0));
                        String s2 = String.valueOf(w.charAt(1));
                        // 只有当单字在白名单中才加入，避免加入 "的", "了" 等
                        if (MEANINGFUL_SINGLE_CHARS.contains(s1)) result.add(s1);
                        if (MEANINGFUL_SINGLE_CHARS.contains(s2)) result.add(s2);
                    }
                    return result.stream();
                }).filter(w -> !STOP_WORDS.contains(w))// 过滤掉停用单字，提高准确率
                .flatMap(w -> {//拼音
                    String fullPinyin = PinYin.toPinYing(w);
                    // 获取首字母，例如 "中国" -> "zg"
                    // 注意：TinyPinyin 的 getFirstLetter 方法，或者你可以自己写逻辑取首字母
                    String firstLetter = PinYin.toShortPinYing(w);

                    // 返回一个包含3种形式的流
                    return Arrays.stream(new String[]{w, fullPinyin, firstLetter});
                })
                .distinct()//去重
                .collect(Collectors.joining(" "));
    }
}
