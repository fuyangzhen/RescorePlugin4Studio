package org.elasticsearch.example.rescore;

import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.plugins.SearchPlugin;

import java.util.List;

import static java.util.Collections.singletonList;

public class CustomizedRescorePlugin extends Plugin implements SearchPlugin {
    @Override
    public List<RescorerSpec<?>> getRescorers() {
        return singletonList(
                new RescorerSpec<>(CustomizedRescorerBuider.NAME, CustomizedRescorerBuider::new, CustomizedRescorerBuider::fromXContent));
    }
}