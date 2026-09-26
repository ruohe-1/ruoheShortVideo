package com.shortvideo.strategy.report;


import com.shortvideo.exceptionHandler.BusinessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class ReportHandlerFactory {
    private final Map<Integer, ReportHandler> handlerMap;

    @Autowired
    public ReportHandlerFactory(List<ReportHandler> handlers) {
        this.handlerMap = handlers.stream()
                .collect(Collectors.toMap(ReportHandler::getTargetType, Function.identity()));
    }
    public ReportHandler getHandler(Integer targetType) {
        ReportHandler handler = handlerMap.get(targetType);
        if (handler == null) {
            throw new BusinessException("不支持的举报类型");
        }
        return handler;
    }

}
