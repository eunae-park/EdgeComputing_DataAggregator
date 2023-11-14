package kr.re.keti;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.security.auth.x500.X500Principal;

import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.util.io.pem.PemReader;

public class Ssl {
	public static Ssl instance = new Ssl();
	public static ConcurrentHashMap<String, String> uuidMap = new ConcurrentHashMap<>();
	public static ConcurrentHashMap<String, PublicKey> pubKeyMap = new ConcurrentHashMap<>();
	private static String certFolder;
	private static PrivateKey privateKey;
	private static String signatureAlgorithm = "SHA256withRSA";
	
	public static Ssl getInstance() {
		return instance;
	}
	private Ssl() {
		
	}
	
	public static void setting(String path, String priKey, String pubKey) {
		certFolder = path;
		
		File keys = new File(path+"keys");
		if(!keys.exists()) keys.mkdir();
		
		path = path.endsWith("/") ? path : "/";
		path += "Private/";
		File Folder = new File(path);
		if(!Folder.exists()) Folder.mkdir();
		String certPath = Folder+"/"+"cert.crt";
		String priKeyPath = Folder+"/"+priKey;
		String pubKeyPath = Folder+"/"+pubKey;
		selfSignedCertificate(certPath, priKeyPath, pubKeyPath, 365);
		
		privateKey = generatePrivateKey(priKeyPath);
	}
	public static ArrayList<String> getKeyList() {
		ArrayList<String> keyList = new ArrayList<>();

        for (Map.Entry<String, String> entry : uuidMap.entrySet()) {
            String address = entry.getKey();
            String uuid = entry.getValue();
            PublicKey key = pubKeyMap.get(uuid);
            String data = Base64.getEncoder().encodeToString(key.getEncoded());
            
            String keyData = address+":"+uuid+":"+data;
            keyList.add(keyData);
        }
		return keyList;
	}
	public static String getUuid(String address) {
		String uuid = uuidMap.get(address);
		return uuid;
	}
	public static void addKey(String address, String uuid, String keyData) {
		uuidMap.put(address, uuid);
		
//		for(Map.Entry<String, String> entry : uuidMap.entrySet()) {
//			String key = entry.getKey();
//			String value = entry.getValue();
//			System.out.println(key+" : "+value);
//		}
		
		String path = certFolder+"keys/"+uuid+".key";
		byte[] data = Base64.getDecoder().decode(keyData);
        try (OutputStream out = new BufferedOutputStream(Files.newOutputStream(Paths.get(path), StandardOpenOption.CREATE))) {
            byte[] buffer = new byte[1024];
            int lengthRead;
            InputStream in = new ByteArrayInputStream(data);
            while ((lengthRead = in.read(buffer)) > 0) {
                out.write(buffer, 0, lengthRead);
                out.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
		PublicKey key = generatePublicKey(path);
		pubKeyMap.put(uuid, key);
	}
	public static String getKey(String address) {
		String keyData = "none";
		String path = "none";
		if(address.equals(Main.deviceIP)) {
			path = certFolder+"Private/pub.key";
		}
		else {
			String uuid = uuidMap.get(address);
			if(uuid == null) {
				System.out.println("[SSL] "+address+"not found");
				return keyData;
			}
			path = certFolder+"keys/"+uuid+".key";			
		}
		byte[] data = null;
	    try {
	        FileInputStream fis = new FileInputStream(path);
	        data = new byte[fis.available()];
	        fis.read(data);
	        fis.close();
	    } catch (IOException e) {
	        e.printStackTrace();
	    }
	    keyData = Base64.getEncoder().encodeToString(data);
		return keyData;
	}
	public static void deleteKey(String address) {
		pubKeyMap.remove(address);
	}
	public static String getPath() {
		return certFolder;
	}
	public void setAlgorithm(String algorithm) {
		signatureAlgorithm = algorithm;
	}
	public String getAlgorithm() {
		return signatureAlgorithm;
	}
	public String sign(String filePath) {
	    String signatureData = "none";
	    try {
	        Signature privateSignature = Signature.getInstance(signatureAlgorithm);
	        privateSignature.initSign(privateKey);

	        try (InputStream in = new FileInputStream(filePath)) {
	            byte[] buffer = new byte[1024];
	            int read;
	            while ((read = in.read(buffer)) != -1) {
	                privateSignature.update(buffer, 0, read);
	            }
	        }

	        byte[] signature = privateSignature.sign();
	        signatureData = Base64.getEncoder().encodeToString(signature);
	    } catch (IOException | NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
	        e.printStackTrace();
	    }
	    return signatureData;
	}
	public static boolean verify(String address, String signature, String filePath) {
		boolean isVerified = false;

        // 검증할 파일을 로드합니다.
		try {
			
			System.out.println("uuidMap");
			for(Map.Entry<String, String> entry : uuidMap.entrySet()) {
				String key = entry.getKey();
				String value = entry.getValue();
				System.out.println(key+" : "+value);
			}
			System.out.println("pubkeyMap");
			for(Map.Entry<String, PublicKey> entry : pubKeyMap.entrySet()) {
				String key = entry.getKey();
				PublicKey value = entry.getValue();
				System.out.println(key+" : " + (value!=null));
			}
			System.out.println("-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=");
			String uuid = uuidMap.get(address);
			PublicKey key = pubKeyMap.get(uuid);
			if(key == null) {
				System.out.println("not find public key");
				return isVerified;
			}
			byte[] signatureData = Base64.getDecoder().decode(signature);
			byte[] data = Files.readAllBytes(Paths.get(filePath));
	        Signature publicSignature = Signature.getInstance(signatureAlgorithm);
	        publicSignature.initVerify(key);
	        publicSignature.update(data);

	        isVerified = publicSignature.verify(signatureData);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InvalidKeyException e) {
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (SignatureException e) {
			e.printStackTrace();
		}

        return isVerified;
	}
	public static PrivateKey generatePrivateKey(String priKeyPath) {
	      PrivateKey privateKey = null;
	      try {
	    	  PemReader pemReader = new PemReader(new FileReader(priKeyPath));
	    	  PEMParser pemParser = new PEMParser(pemReader);
		      Object object = pemParser.readObject();
		      JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider("BC");
	
		      if (object instanceof PEMKeyPair) {
		          KeyPair kp = converter.getKeyPair((PEMKeyPair) object);
		          privateKey = kp.getPrivate();
		      } else if (object instanceof PrivateKeyInfo) {
		          privateKey = converter.getPrivateKey((PrivateKeyInfo) object);
		      }
		      pemParser.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
      return privateKey;
	}
	public static PublicKey generatePublicKey(String pubKeyPath) {
		PublicKey pubKey = null;
		try {
			String publicKeyPEM = new String(Files.readAllBytes(Paths.get(pubKeyPath)));
			publicKeyPEM = publicKeyPEM.replace("-----BEGIN PUBLIC KEY-----\n", "");
			publicKeyPEM = publicKeyPEM.replace("-----END PUBLIC KEY-----", "");
			publicKeyPEM = publicKeyPEM.replace("\n", "");
			byte[] encodedPublicKey = Base64.getDecoder().decode(publicKeyPEM);

			X509EncodedKeySpec spec = new X509EncodedKeySpec(encodedPublicKey);
			KeyFactory kf = KeyFactory.getInstance("RSA");
			pubKey = kf.generatePublic(spec);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (InvalidKeySpecException e) {
			e.printStackTrace();
		}

		return pubKey;
	}
	public static void selfSignedCertificate(String crtPath, String priKeyPath, String pubKeyPath, long expirationDate) {
		selfSignedCertificate(crtPath, priKeyPath, pubKeyPath, expirationDate, "SHA256withRSA");
	}
	public static void selfSignedCertificate(String crtPath, String priKeyPath, String pubKeyPath, long expirationDate, String signatureAlgorithm) {
		try {
			if(!new File(crtPath).exists() || ! new File(priKeyPath).exists() || new File(pubKeyPath).exists()) {
				Security.addProvider(new BouncyCastleProvider());
				
		        KeyPairGenerator keyPairGenerator;
					keyPairGenerator = KeyPairGenerator.getInstance("RSA");
		        keyPairGenerator.initialize(2048, new SecureRandom());
		        KeyPair keyPair = keyPairGenerator.generateKeyPair();
		
		        Date notBefore = new Date();
		//        Date notAfter = new Date(notBefore.getTime() + 3650L * 24 * 60 * 60 * 1000); // 10 years
		     // 현재 날짜를 가져옵니다.
		        LocalDate now = LocalDate.now();
		
		        // 10년 후의 날짜를 계산합니다.
		        LocalDate tenYearsLater = now.plusDays(expirationDate);
		
		        // LocalDate를 Date로 변환합니다.
		        Instant instant = tenYearsLater.atStartOfDay(ZoneId.systemDefault()).toInstant();
		        
		        Date notAfter = Date.from(instant);
		
		        JcaX509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
		            new X500Principal("CN=SelfSigned"), BigInteger.ONE, notBefore, notAfter,
		            new X500Principal("CN=SelfSigned"), keyPair.getPublic());
		
		        ContentSigner signer = new JcaContentSignerBuilder(signatureAlgorithm).build(keyPair.getPrivate());
		        X509Certificate cert = new JcaX509CertificateConverter().getCertificate(certBuilder.build(signer));
		
		        // 인증서를 PEM 형식으로 변환
		        String certPem = "-----BEGIN CERTIFICATE-----\n";
		        certPem += Base64.getEncoder().encodeToString(cert.getEncoded()).replaceAll("(.{64})", "$1\n");
		        certPem += "\n-----END CERTIFICATE-----\n";
		
		        // 개인 키를 PEM 형식으로 변환
		        PrivateKey privateKey = keyPair.getPrivate();
		        String keyPem = "-----BEGIN PRIVATE KEY-----\n";		        
		        keyPem += Base64.getEncoder().encodeToString(privateKey.getEncoded()).replaceAll("(.{64})", "$1\n");
		        keyPem += "\n-----END PRIVATE KEY-----\n";
		        // PEM 형식의 인증서와 개인 키를 파일로 저장
		        Files.write(Paths.get(crtPath), certPem.getBytes());
		        Files.write(Paths.get(priKeyPath), keyPem.getBytes());
		        // 인증서에서 공개 키를 추출합니다.
		        PublicKey publicKey = cert.getPublicKey();

		        // 공개 키를 PEM 형식으로 변환합니다.
		        String publicKeyPEM = "-----BEGIN PUBLIC KEY-----\n";
		        publicKeyPEM += Base64.getEncoder().encodeToString(publicKey.getEncoded()).replaceAll("(.{64})", "$1\n");
		        publicKeyPEM += "\n-----END PUBLIC KEY-----\n";

		        // PEM 형식의 공개 키를 파일로 저장합니다.
		        Files.write(Paths.get(pubKeyPath), publicKeyPEM.getBytes());
			}
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (OperatorCreationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (CertificateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public static String sign(String priKeyPath, String filePath) {
		return sign(priKeyPath, filePath, "SHA256withRSA");
	}
	public static String sign(String priKeyPath, String filePath, String signatureAlgorithm) {
		String signatureData = "none";
        // 개인 키 파일을 로드합니다.
		try {
			PemReader pemReader = new PemReader(new FileReader(priKeyPath));
	        PEMParser pemParser = new PEMParser(pemReader);
	        Object object = pemParser.readObject();
	        JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider("BC");
	        PrivateKey privateKey = null;

	        if (object instanceof PEMKeyPair) {
	            KeyPair kp = converter.getKeyPair((PEMKeyPair) object);
	            privateKey = kp.getPrivate();
	        } else if (object instanceof PrivateKeyInfo) {
	            privateKey = converter.getPrivateKey((PrivateKeyInfo) object);
	        }
	     // 텍스트 파일을 로드합니다.
	        byte[] data = Files.readAllBytes(Paths.get(filePath));

	        // SHA-256 해시를 계산하고, 이를 개인 키로 서명합니다.
	        Signature privateSignature = Signature.getInstance(signatureAlgorithm);
	        privateSignature.initSign(privateKey);
	        privateSignature.update(data);
	        byte[] signature = privateSignature.sign();
	        signatureData = Base64.getEncoder().encodeToString(signature);

	        pemParser.close();
////	         서명을 파일로 직접 저장합니다.
//	        Files.write(Paths.get("/home/keti/cert/sign.crt"), signature);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (InvalidKeyException e) {
			e.printStackTrace();
		} catch (SignatureException e) {
			e.printStackTrace();
		}
        
        return signatureData;
	}
	public static boolean verify(byte[] signature, String pubKeyPath, String filePath) {
		return verify(signature, pubKeyPath, filePath, "SHA256withRSA");
	}
	public static boolean verify(byte[] signature, String pubKeyPath, String filePath, String signatureAlgorithm) {
		boolean isVerified = false;

	     // 공개 키 파일을 로드합니다.
			try {
				String publicKeyPEM = new String(Files.readAllBytes(Paths.get(pubKeyPath)));
		        // PEM 형식에서 불필요한 헤더와 푸터를 제거합니다.
		        publicKeyPEM = publicKeyPEM.replace("-----BEGIN PUBLIC KEY-----\n", "");
		        publicKeyPEM = publicKeyPEM.replace("-----END PUBLIC KEY-----", "");
		        publicKeyPEM = publicKeyPEM.replace("\n", "");
		        // Base64로 인코딩된 키를 디코딩합니다.
		        byte[] encodedPublicKey = Base64.getDecoder().decode(publicKeyPEM);

		        // 바이트 배열을 PublicKey 객체로 변환합니다.
		        X509EncodedKeySpec spec = new X509EncodedKeySpec(encodedPublicKey);
		        KeyFactory kf = KeyFactory.getInstance("RSA");
		        PublicKey publicKey = kf.generatePublic(spec);

		        // 서명 파일을 로드합니다.
//		        byte[] signature = Files.readAllBytes(Paths.get(signPath));

		        // 검증할 파일을 로드합니다.
		        byte[] data = Files.readAllBytes(Paths.get(filePath)); // 실제 파일 경로로 대체하세요.

		        // SHA-256 해시를 계산하고, 이를 공개 키로 검증합니다.
		        Signature publicSignature = Signature.getInstance(signatureAlgorithm);
		        publicSignature.initVerify(publicKey);
		        publicSignature.update(data);

		        isVerified = publicSignature.verify(signature);
			} catch (IOException e) {
				e.printStackTrace();
			} catch (NoSuchAlgorithmException e) {
				e.printStackTrace();
			} catch (InvalidKeySpecException e) {
				e.printStackTrace();
			} catch (InvalidKeyException e) {
				e.printStackTrace();
			} catch (SignatureException e) {
				e.printStackTrace();
			}
		return isVerified;
	}
}
