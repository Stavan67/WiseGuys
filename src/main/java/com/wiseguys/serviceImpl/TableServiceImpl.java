package com.wiseguys.serviceImpl;

import com.wiseguys.entity.CafeTable;
import com.wiseguys.repository.CafeTableRepository;
import com.wiseguys.serviceI.QrCodeService;
import com.wiseguys.serviceI.TableService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@RequiredArgsConstructor
public class TableServiceImpl implements TableService {
    private final CafeTableRepository tableRepository;
    private final QrCodeService qrCodeService;

    @Override
    public List<CafeTable> listTables() {
        return tableRepository.findAll();
    }

    @Override
    public CafeTable createTable(String tableNumber) {
        CafeTable table = new CafeTable();
        table.setTableNumber(tableNumber);
        table.setQrCodeValue(UUID.randomUUID().toString());
        return tableRepository.save(table);
    }

    @Override
    public byte[] getQrCodePng(String tableNumber) {
        CafeTable table = tableRepository.findByTableNumber(tableNumber)
                .orElseThrow(() -> new IllegalArgumentException("Invalid table: " + tableNumber));
        return qrCodeService.generateQrPng(table.getQrCodeValue(), "Table " + table.getTableNumber(), 500);
    }

    @Override
    public byte[] downloadAllQrCodesZip() throws IOException {
        List<CafeTable> tables = tableRepository.findAll();
        ByteArrayOutputStream zipBuffer = new ByteArrayOutputStream();

        try (ZipOutputStream zos = new ZipOutputStream(zipBuffer)) {
            for (CafeTable table : tables) {
                byte[] png = qrCodeService.generateQrPng(table.getQrCodeValue(), "Table " + table.getTableNumber(), 500);
                zos.putNextEntry(new ZipEntry(table.getTableNumber() + ".png"));
                zos.write(png);
                zos.closeEntry();
            }
        }

        return zipBuffer.toByteArray();
    }
}