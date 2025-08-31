package store.lastdance.converter.analysis;

import org.springframework.stereotype.Component;
import org.springframework.core.convert.converter.Converter;
import store.lastdance.domain.analysis.FeedbackType;

@Component
public class StringToFeedbackTypeConverter implements Converter<String, FeedbackType> {

    @Override
    public FeedbackType convert(String source) {
        if (source == null) {
            return null;
        }
        // 대소문자 구분없이 변환
        return FeedbackType.valueOf(source.toUpperCase());
    }
}