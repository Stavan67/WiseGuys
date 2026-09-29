package com.wiseguys.controller;

import com.wiseguys.dto.AdminTableResponse;
import com.wiseguys.entity.CafeTable;
import com.wiseguys.serviceI.QrCodeService;
import com.wiseguys.serviceI.TableService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/tables")
@RequiredArgsConstructor
public class AdminTableController {

    private final TableService tableService;
    private final QrCodeService qrCodeService;

    @GetMapping
    public List<AdminTableResponse> listTables() {
        return tableService.listTables().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @PostMapping
    public ResponseEntity<?> createTable(@RequestBody CreateTableRequest request) {
        if (request.getTableNumber() == null || request.getTableNumber().isBlank()) {
            return ResponseEntity.badRequest().body("Table number is required.");
        }
        CafeTable table = tableService.createTable(request.getTableNumber().trim());
        return ResponseEntity.ok(toResponse(table));
    }

    @GetMapping(value = "/{tableNumber}/qr", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> getQrCode(@PathVariable String tableNumber) {
        byte[] png = tableService.getQrCodePng(tableNumber);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"table-" + tableNumber + "-qr.png\"")
                .body(png);
    }

    @GetMapping("/qr/all")
    public ResponseEntity<byte[]> downloadAllQrCodes() throws IOException {
        byte[] zip = tableService.downloadAllQrCodesZip();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"wiseguys-table-qr-codes.zip\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(zip);
    }

    private AdminTableResponse toResponse(CafeTable table) {
        return new AdminTableResponse(
                table.getId(),
                table.getTableNumber(),
                table.isActive(),
                qrCodeService.resolveMenuUrl(table.getQrCodeValue())
                );
    }

    public static class CreateTableRequest {
        private String tableNumber;

        public String getTableNumber() {
            return tableNumber;
        }

        public void setTableNumber(String tableNumber) {
            this.tableNumber = tableNumber;
        }
    }
}