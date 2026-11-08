package dev.zhulidov.cash_tracker.transactions.importpdf;


import dev.zhulidov.cash_tracker.transactions.dto.ReviewedTransactionRequest;
import dev.zhulidov.cash_tracker.transactions.dto.TransactionCreateRequest;
import dev.zhulidov.cash_tracker.transactions.dto.TransactionDto;
import dev.zhulidov.cash_tracker.transactions.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.List;


@Service
@RequiredArgsConstructor
public class PacketTransactionService {


    private final TransactionService transactionService;

    @Transactional
    public List<TransactionDto> createPacketTransactions(ReviewedTransactionRequest requests , Long userId){

        List<TransactionDto> transactions = new ArrayList<>();
        for (TransactionCreateRequest req: requests.transactions()){
           TransactionDto dto = transactionService.createTransaction(req,userId);
           transactions.add(dto);
        }
        return transactions;
    }



}
