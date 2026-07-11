package src.document.service;

import org.springframework.stereotype.Component;

@Component
public class TokenCountEstimator {

    public int estimate(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }

        return Math.max(
                1,
                (int) Math.ceil(text.length() / 4.0)
        );
    }
}
