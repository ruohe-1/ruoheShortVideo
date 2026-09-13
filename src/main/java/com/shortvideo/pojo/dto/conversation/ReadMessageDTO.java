package com.shortvideo.pojo.dto.conversation;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReadMessageDTO {
    private Long lastReadMessageId;
}
