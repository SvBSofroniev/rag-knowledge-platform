package src.document.service;

import org.springframework.stereotype.Component;

@Component
public class TokenCountEstimator {

    public int estimate(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }

        /*
         * Temporary approximation:
         * one token is roughly four characters for English text.
         *
         * Later, replace this with the tokenizer used by
         * the selected embedding model.
         */
        return Math.max(
                1,
                (int) Math.ceil(text.length() / 4.0)
        );
    }
}