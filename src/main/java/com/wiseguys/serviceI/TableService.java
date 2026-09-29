package com.wiseguys.serviceI;

import com.wiseguys.entity.CafeTable;

import java.io.IOException;
import java.util.List;

public interface TableService {
    List<CafeTable> listTables();
    CafeTable createTable(String tableNumber);
    byte[] getQrCodePng(String tableNumber);
    byte[] downloadAllQrCodesZip() throws IOException;
}