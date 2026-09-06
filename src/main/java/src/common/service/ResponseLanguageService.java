package src.common.service;

import org.springframework.stereotype.Service;
import src.common.language.ResponseLanguage;

@Service
public class ResponseLanguageService {

    public ResponseLanguage detect(
            String text
    ) {
        if (text == null ||
                text.isBlank()) {

            return ResponseLanguage.ENGLISH;
        }

        long cyrillicCharacters =
                text.codePoints()
                        .filter(character ->
                                character >= '\u0400' &&
                                        character <= '\u04FF'
                        )
                        .count();

        long latinCharacters =
                text.codePoints()
                        .filter(character ->
                                (character >= 'A' &&
                                        character <= 'Z') ||
                                        (character >= 'a' &&
                                                character <= 'z')
                        )
                        .count();

        if (cyrillicCharacters >
                latinCharacters) {

            return ResponseLanguage.BULGARIAN;
        }

        return ResponseLanguage.ENGLISH;
    }
}