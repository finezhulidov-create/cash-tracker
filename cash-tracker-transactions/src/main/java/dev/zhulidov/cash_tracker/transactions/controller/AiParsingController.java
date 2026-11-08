package dev.zhulidov.cash_tracker.transactions.controller;

import dev.zhulidov.cash_tracker.common.security.UserPrincipal;
import dev.zhulidov.cash_tracker.transactions.dto.ImportPreviewItem;
import dev.zhulidov.cash_tracker.transactions.dto.ReviewedTransactionRequest;
import dev.zhulidov.cash_tracker.transactions.dto.TransactionDto;
import dev.zhulidov.cash_tracker.transactions.importpdf.AIParsingService;
import dev.zhulidov.cash_tracker.transactions.importpdf.PacketTransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/v1/parsing")
@RequiredArgsConstructor
public class AiParsingController {

        private final AIParsingService parsingService;
        private final PacketTransactionService packetTransactionService;

        /*Отправляет запрос ллм на составление джейсона с транзакциями
        Возвращает джейсон с вытащенными операциями для редактирования пользователем
        */
        @PostMapping
        public ResponseEntity<List<ImportPreviewItem>> parsePDF(@RequestParam("file") MultipartFile multipartFile,
                                                                @AuthenticationPrincipal UserPrincipal principal){
            return ResponseEntity.ok(parsingService.generateTransactions(multipartFile,principal.getId()));
        }

        /*Получает список отредактированных пользователем операций с фронта
        сохраняет их пакетно в бд и возвращает список дтошек
        * */
        @PostMapping("/save")
        public ResponseEntity<List<TransactionDto>> createTransactions(@RequestBody @Valid ReviewedTransactionRequest requests,
                                                                       @AuthenticationPrincipal UserPrincipal me){
            return ResponseEntity.ok(packetTransactionService.createPacketTransactions(requests,me.getId()));
        }




}
