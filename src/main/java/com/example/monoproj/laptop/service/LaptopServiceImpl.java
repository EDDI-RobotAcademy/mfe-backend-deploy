package com.example.monoproj.laptop.service;

import com.example.monoproj.account.entity.Account;
import com.example.monoproj.account.repository.AccountRepository;
import com.example.monoproj.laptop.entity.Laptop;
import com.example.monoproj.laptop.entity.LaptopImage;
import com.example.monoproj.laptop.entity.LaptopImageType;
import com.example.monoproj.laptop.repository.LaptopImageRepository;
import com.example.monoproj.laptop.repository.LaptopRepository;
import com.example.monoproj.laptop.service.request.RegisterLaptopImageRequest;
import com.example.monoproj.laptop.service.request.RegisterLaptopRequest;
import com.example.monoproj.laptop.service.response.RegisterLaptopResponse;
import com.example.monoproj.utility.S3Uploader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LaptopServiceImpl implements LaptopService {

    private final LaptopImageRepository laptopImageRepository;
    private final LaptopRepository laptopRepository;
    private final AccountRepository accountRepository;
    private final S3Uploader s3Uploader;

    @Override
    @Transactional
    public RegisterLaptopResponse registerLaptop(
            RegisterLaptopRequest laptopRequest,
            RegisterLaptopImageRequest laptopImageRequest) throws IOException {

        // 1. 등록자 정보 확인
        Account account = accountRepository.findById(laptopRequest.getAccountId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        // 2. Laptop 저장
        Laptop requestedLaptop = laptopRequest.toLaptop(account);
        Laptop savedLaptop = laptopRepository.save(requestedLaptop);

        // 3. 이미지 업로드 및 저장

        // 썸네일 업로드 및 변환
        String thumbnailUrl = s3Uploader.upload(laptopImageRequest.getThumbnailFile(), "laptops");
        LaptopImage thumbnailImage = laptopImageRequest.toLaptopImageThumbnail(thumbnailUrl, savedLaptop);
        LaptopImage savedThumbnailImage = laptopImageRepository.save(thumbnailImage);

        // 상세 이미지 업로드 및 변환
        List<String> detailImageUrls = new ArrayList<>();
        for (MultipartFile file : laptopImageRequest.getImageFileList()) {
            detailImageUrls.add(s3Uploader.upload(file, "laptops"));
        }
        List<LaptopImage> detailImages = laptopImageRequest.toLaptopImageList(detailImageUrls, savedLaptop);
        List<LaptopImage> savedDetailImages = laptopImageRepository.saveAll(detailImages);

        // 4. 응답 구성
        List<LaptopImage> allImages = new ArrayList<>();
        allImages.add(savedThumbnailImage);
        allImages.addAll(savedDetailImages);

        return RegisterLaptopResponse.from(savedLaptop, allImages);
    }
}
