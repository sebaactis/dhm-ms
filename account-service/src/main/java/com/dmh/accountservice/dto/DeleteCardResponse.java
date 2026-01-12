package com.dmh.accountservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para respuesta de eliminación de tarjeta.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeleteCardResponse {

    private String message;
    private Long cardId;
    private Long accountId;
    private String status;

    /**
     * Método helper para crear respuesta exitosa.
     */
    public static DeleteCardResponse success(Long accountId, Long cardId) {
        return DeleteCardResponse.builder()
                .message("Card deleted successfully")
                .cardId(cardId)
                .accountId(accountId)
                .status("DELETED")
                .build();
    }
}
