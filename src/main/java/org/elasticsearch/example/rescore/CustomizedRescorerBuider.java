/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.example.rescore;

import org.apache.lucene.document.Document;

import org.apache.lucene.index.Term;

import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;

import org.elasticsearch.common.ParseField;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.xcontent.ConstructingObjectParser;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentParser;

import org.elasticsearch.index.query.QueryRewriteContext;
import org.elasticsearch.index.query.QueryShardContext;
import org.elasticsearch.search.rescore.RescoreContext;
import org.elasticsearch.search.rescore.Rescorer;
import org.elasticsearch.search.rescore.RescorerBuilder;

import java.io.IOException;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.Collections.singletonList;
import static org.elasticsearch.common.xcontent.ConstructingObjectParser.constructorArg;


/**
 * Example rescorer that multiplies the score of the hit by some factor and doesn't resort them.
 */
public class CustomizedRescorerBuider extends RescorerBuilder<CustomizedRescorerBuider> {
    public static final String NAME = "plugin_parameters";

    private final ArrayList<Float> queryFactors; // delete final
    private final ArrayList<String> queryKeywords;
    private final ArrayList<Float> queryEmbedding;


    public CustomizedRescorerBuider(ArrayList<Float> queryFactors,
                                    ArrayList<String> queryKeywords,
                                    ArrayList<Float> queryEmbedding) {
        this.queryFactors = queryFactors;
        this.queryKeywords = queryKeywords;
        this.queryEmbedding = queryEmbedding;
    }

    private ArrayList<Float> convertFloatArraytoList(float[] in) {
        ArrayList<Float> out = new ArrayList<Float>();
        for (float i : in) {
            out.add(i);

        }
        return out;
    }

    private ArrayList<String> convertStringArraytoList(String[] in) {
        ArrayList<String> out = new ArrayList<String>();
        for (String i : in) {
            out.add(i);

        }
        return out;
    }

    CustomizedRescorerBuider(StreamInput in) throws IOException {
        super(in);
        queryFactors = convertFloatArraytoList(in.readFloatArray());
        queryKeywords = convertStringArraytoList(in.readStringArray());
        queryEmbedding = convertFloatArraytoList(in.readFloatArray());
    }

    @Override
    protected void doWriteTo(StreamOutput out) throws IOException {
//        out.writeFloat(factor);
//        out.writeFloatArray(.toArray());
//        out.writeFloatArray(entity_factor.toArray());
        System.err.println(">>>>>>>>> doWriteTo(StreamOutput out) <<<<<<<<<<<<<<");

    }

    @Override
    public String getWriteableName() {
        return NAME;
    }

    @Override
    public RescorerBuilder<CustomizedRescorerBuider> rewrite(QueryRewriteContext ctx) throws IOException {
        return this;
    }

    private static final ParseField QUERY_FACTORS = new ParseField("queryFactors");
    private static final ParseField QUERY_KEYWORDS = new ParseField("queryKeywords");
    private static final ParseField QUERY_EMBEDDING = new ParseField("queryEmbedding"); // TODO: this is the field name, set it with default null


    @Override
    protected void doXContent(XContentBuilder builder, Params params) throws IOException {
        builder.field(QUERY_FACTORS.getPreferredName(), queryFactors);
        builder.field(QUERY_KEYWORDS.getPreferredName(), queryKeywords);
        builder.field(QUERY_EMBEDDING.getPreferredName(), queryEmbedding);
    }

    private static final ConstructingObjectParser<CustomizedRescorerBuider, Void> PARSER = new ConstructingObjectParser<>(NAME,
            args -> new CustomizedRescorerBuider((ArrayList<Float>) args[0], (ArrayList<String>) args[1], (ArrayList<Float>) args[2]));

    static {
        PARSER.declareFloatArray(constructorArg(), QUERY_FACTORS); // TODO: tune the declare func to parse hashmap
        PARSER.declareStringArray(constructorArg(), QUERY_KEYWORDS);
        PARSER.declareFloatArray(constructorArg(), QUERY_EMBEDDING);

    }

    public static CustomizedRescorerBuider fromXContent(XContentParser parser) {
        return PARSER.apply(parser, null);
    }

    @Override
    public RescoreContext innerBuildContext(int windowSize, QueryShardContext context) throws IOException {
        //windowSize entrance!!!
        return new ExampleRescoreContext(windowSize, queryFactors, queryKeywords, queryEmbedding);
    }

    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj)) {
            return false;
        }
        CustomizedRescorerBuider other = (CustomizedRescorerBuider) obj;
        return queryFactors == other.queryFactors
                && queryKeywords == other.queryKeywords
                && queryEmbedding == other.queryEmbedding;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), queryFactors, queryKeywords, queryEmbedding);
    }

    ArrayList<Float> queryFactors() {
        return queryFactors;
    }

    ArrayList<String> queryKeywords() {
        return queryKeywords;
    }

    ArrayList<Float> queryEmbedding() {
        return queryEmbedding;
    }


    private static class ExampleRescoreContext extends RescoreContext {
        private final ArrayList<Float> queryFactors;
        private final ArrayList<String> queryKeywords;
        private final ArrayList<Float> queryEmbedding;


        ExampleRescoreContext(int windowSize, ArrayList<Float> queryFactors,
                              ArrayList<String> queryKeywords,
                              ArrayList<Float> queryEmbedding) {
            super(windowSize, ExampleRescorer.INSTANCE);
            this.queryFactors = queryFactors;
            this.queryKeywords = queryKeywords;
            this.queryEmbedding = queryEmbedding;
        }
    }

    private static void debugInfo(String key, String value) {
        System.err.println("===[From Studio Plugin]=== :: " + " *" + key + "* : " + value);
    }

    private static class ExampleRescorer implements Rescorer {
        private static final ExampleRescorer INSTANCE = new ExampleRescorer();

        @Override
        public TopDocs rescore(TopDocs topDocs, IndexSearcher searcher, RescoreContext rescoreContext) throws IOException {
            ExampleRescoreContext context = (ExampleRescoreContext) rescoreContext;
            int end = Math.min(topDocs.scoreDocs.length, rescoreContext.getWindowSize());
            /*
             * Since this example looks up a single field value it should
             * access them in docId order because that is the order in
             * which they are stored on disk and we want reads to be
             * forwards and close together if possible.
             *
             * If accessing multiple fields we'd be better off accessing
             * them in (reader, field, docId) order because that is the
             * order they are on disk.
             */

            for (int i = 0; i < end; i++) {

                Document doc = searcher.doc(topDocs.scoreDocs[i].doc);
                //------------------get DB field array--------
                Float keywordsScore = editDistance(context.queryKeywords, listStringFieldValue(doc, "keywords"));
                Float embeddingScore = vectorDistance(context.queryEmbedding, listFloatFieldValue(doc, "embedding"));

                List<MyStruct> factorScore = new ArrayList<MyStruct>() {
                    {
                        add(new MyStruct("keywords Score", keywordsScore));
                        add(new MyStruct("embedding Score", embeddingScore));
                    }
                };
                debugInfo("--> doc [ " + i + " ] init score = ", String.valueOf(topDocs.scoreDocs[i].score));
                for (int j = 0; j < factorScore.size(); j++) {
                    System.err.printf("[%-20s] %-7f * %7f [factor]\n", factorScore.get(j).name, factorScore.get(j).value, context.queryFactors.get(j));
                    topDocs.scoreDocs[i].score += factorScore.get(j).value * context.queryFactors.get(j);
                }

                debugInfo("^^^ doc [ " + i + " ] NEW score = ", String.valueOf(topDocs.scoreDocs[i].score));
            }

            // Sort by score descending, then docID ascending, just like lucene's QueryRescorer
            Arrays.sort(topDocs.scoreDocs, (a, b) -> {
                if (a.score > b.score) {
                    return -1;
                }
                if (a.score < b.score) {
                    return 1;
                }
                // Safe because doc ids >= 0
                return a.doc - b.doc;
            });
            int shardLength = topDocs.scoreDocs.length;
            int getLength = (int) (shardLength > context.queryFactors.get(context.queryFactors.size() - 1) ? context.queryFactors.get(context.queryFactors.size() - 1) : shardLength);
            ScoreDoc[] newScoreDoc = new ScoreDoc[getLength];
            System.arraycopy(topDocs.scoreDocs, 0, newScoreDoc, 0, getLength);
            TopDocs newTopDocs = new TopDocs(topDocs.totalHits, newScoreDoc, topDocs.getMaxScore());
            debugInfo("this shard's docs amount ", String.valueOf(shardLength));
            debugInfo("script return docs amount ", newTopDocs.scoreDocs.length + "\n");
            return newTopDocs;
        }

        @Override
        public Explanation explain(int topLevelDocId, IndexSearcher searcher, RescoreContext rescoreContext,
                                   Explanation sourceExplanation) throws IOException {
            ExampleRescoreContext context = (ExampleRescoreContext) rescoreContext;
            // Note that this is inaccurate because it ignores factor field
            return Explanation.match(-1, ":::our plugin uses keywords list's edit distance to rescore candidates", singletonList(sourceExplanation));
        }

        @Override
        public void extractTerms(IndexSearcher searcher, RescoreContext rescoreContext, Set<Term> termsSet) {
            // Since we don't use queries there are no terms to extract.
        }

        public Float editDistance(List<String> word1, List<String> word2) {
            System.err.println("word1: "+word1);
            System.err.println("word2: "+word2);
            if (word1.size() == 0 || word2.size() == 0) {
                return 0.0F;
            }
            Set set1 = new HashSet(word1);
            Set set2 = new HashSet(word2);

            set1.retainAll(set2);
            if (set1.isEmpty()) {
                return 0.0F;
            }
            int[][] dp = new int[word1.size() + 1][word2.size() + 1];
            for (int i = 0; i < dp.length; i++) {
                dp[i][0] = i;
            }
            for (int j = 0; j < dp[0].length; j++) {
                dp[0][j] = j;
            }

            for (int i = 1; i < dp.length; i++) {
                for (int j = 1; j < dp[0].length; j++) {
                    if (word1.get(i - 1).equals(word2.get(j - 1))) {
                        dp[i][j] = dp[i - 1][j - 1];
                    } else {
                        // 插入或者删除word1的字符
                        int insert = dp[i - 1][j] + 1;
                        int delete = dp[i][j - 1] + 1;
                        int replace = dp[i - 1][j - 1] + 1;
                        dp[i][j] = Math.min(insert, Math.min(delete, replace));
                    }
                }
            }
            return 10.0F - (dp[word1.size()][word2.size()] > 0.0F ? dp[word1.size()][word2.size()] : 0.0F);
        }

        public List<String> listStringFieldValue(Document doc, String field) {
            String[] values = doc.getValues(field);
            List stringList = Arrays.asList(values);
            return stringList;
        }

        public List<Float> listFloatFieldValue(Document doc, String field) {
            String[] values = doc.getValues(field);
            return Arrays.stream(values).map(Float::parseFloat).collect(Collectors.toList());
        }

        public Float vectorDistance(List<Float> queryVec, List<Float> docVec) {
            System.err.println("queryVec: "+queryVec);
            System.err.println("docVec: "+docVec);
            if ((queryVec == null) || (docVec == null) || (queryVec.size() != docVec.size())) {
                return 0.0F;
            }
            double queryVecNorm = 0.0F;
            double docVecNorm = 0.0F;
            Float score = 0.0F;
            for (int i = 0; i < docVec.size(); i++) {
                docVecNorm += Math.pow(docVec.get(i), 2.0);
                queryVecNorm += Math.pow(queryVec.get(i), 2.0);
                score += docVec.get(i) * queryVec.get(i);
            }
            if (docVecNorm == 0 || queryVecNorm == 0) return 0.0F;
            return 10.0F - (score / (float) (Math.sqrt(queryVecNorm) * Math.sqrt(docVecNorm))); //TODO decide the score method
        }
    }

    static class MyStruct {
        String name;
        Float value;

        public MyStruct(String name, Float value) {
            this.name = name;
            this.value = value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            MyStruct myStruct = (MyStruct) o;
            return Objects.equals(name, myStruct.name) &&
                    Objects.equals(value, myStruct.value);
        }

        @Override
        public int hashCode() {

            return Objects.hash(name, value);
        }
    }
}
