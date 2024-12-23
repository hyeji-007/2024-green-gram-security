package com.green.greengram.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

@Slf4j
@Component // 빈등록: spring container에게 객체 관리를 해달라고 하는 것 >> 필요한 것: 생성자 호출(생성자: class와 이름이 똑같고 return 타입을 적지 않는다.)
// 그리고 그 주소값을 가지고 있다가 필요할 때 달라고 하면 준다.
// 기본적으로 싱글톤(객체를 딱 하나만 만든다), shallow copy(항상 같은 값을 준다)
public class MyFileUtils {
    private final String uploadPath;

    public String getUploadPath() {
        return uploadPath;
    }
/*
    @Value("${file.directory}")은
    yaml 파일에 있는 file.directory 속성에 저장된 값을 생성자 호출할 때 값을 넣어준다.
     */

    public MyFileUtils(@Value("${file.directory}") String uploadPath) { //빈등록하면 뭘 넣어야하는지 spring에게 알려줘야 함, @AllArgsConstructor 사용 불가
        log.info("MyFileUtils - 생성자: {}", uploadPath);
        this.uploadPath = uploadPath;
    }

    // path = "ddd/aaa"
    // D:\khj\2024-02\download\greengram_ver1/ddd/aaa >> uploadPath에 저장
    // 디렉토리 생성
    // 확장자가 없으면 디렉토리일 가능성이 크다
    public String makeFolders(String path) { //path: service에서 만든 middle path
        File file = new File(uploadPath, path); //생성자 호출 >> 1개(full 경로) & 2개(경로를 합쳐준다.)

        // static 아님 >> 객체화하고 주소값.(file.) 으로 호출했기 때문에
        // 리턴타입은 boolean >> if()안에서 호출했기 때문에
        // 파라미터는 없음 >> 호출 때 인자를 보내지 않았기 때문에
        // 메소드명은 >> exists였다.

        if(!file.exists()) {
            file.mkdirs();
        }
        return file.getAbsolutePath(); // AbsolutePath: 절대 경로(uploadPath + path)

    }

    //파일명에서 확장자 추출
    public String getExt(String fileName) {
        int lastIdx = fileName.lastIndexOf(".");
        return fileName.substring(lastIdx);
    }

    // 랜덤파일명 생성
    // 상속받은 메소드 재정의 >> 오버라이딩
    // 똑같은 메소드명 여러개 만듦 >> 오버로딩(파라미터가 다르면 ok, 호출할 때 구분할 수 있어야 한다.)
    public String makeRandomFileName() {
        return UUID.randomUUID().toString();
    }

    //랜덤파일명 + 확장자 생성
    public String makeRandomFileName(String originalFileName) {
        return makeRandomFileName() + getExt(originalFileName);
    }

    //업로드된 파일의 이름에서 확장자를 포함한 새로운 랜덤 파일명을 생성
    public String makeRandomFileName(MultipartFile file) {
        return makeRandomFileName(file.getOriginalFilename()); //확장자명 얻기 위해, getOriginalFilename(): return 메소드
    }

    //파일을 원하는 경로에 저장 (호출자마다 사용 방식이 다르니 예외 처리)
    public void transferTo(MultipartFile mf, String path) throws IOException {
        File file = new File(uploadPath, path); //파일 객체 생성: 두 값을 결합하여 전체 경로를 만들기
        mf.transferTo(file); //MultipartFile의 데이터를 지정된 file 위치로 복사
        // 클라이언트가 업로드한 파일을 서버 파일 시스템에 저장하는 작업을 수행합
    }

    //폴더 삭제, e.g. "user/1"
    public void deleteFolder(String path, boolean deleteRootFolder) {
        File folder = new File(path);
        if(folder.exists() && folder.isDirectory()) { //폴더가 존재하면서 디렉토리인가?
            File[] includedFiles = folder.listFiles();

            for (File f : includedFiles) {
                if(f.isDirectory()) {
                    deleteFolder(f.getAbsolutePath(), true); //재귀적으로 하위 디렉토리까지 포함하여 삭제
                } else {
                    f.delete(); //항목이 파일인 경우에는 해당 파일을 삭제함
                }
            }

            if(deleteRootFolder) { //재귀적으로 하위 디렉토리 및 파일을 모두 삭제한 후 "현재 디렉토리"를 삭제할지 결정
                folder.delete(); //현재 디렉토리도 삭제
            }
        }
    }
}

//class Test {
//    public static void main(String[] args) {
//        MyFileUtils myFileUtils = new MyFileUtils("C:/temp");
//        String randomFileName = myFileUtils.makeRandomFileName("119255016.1.jpg");
//        System.out.println(randomFileName);
//    }
//}
