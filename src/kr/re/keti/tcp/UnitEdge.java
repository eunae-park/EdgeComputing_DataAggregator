package kr.re.keti.tcp;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import kr.re.keti.Main;
import kr.re.keti.PortNum;
import kr.re.keti.database.Database;
import kr.re.keti.database.FileManagementDto;

public class UnitEdge{
	private String address;
    private List<String> chunkList;

    public UnitEdge(Database database, String address, String dataid, int startIdx, int finishIdx) {
}
}
