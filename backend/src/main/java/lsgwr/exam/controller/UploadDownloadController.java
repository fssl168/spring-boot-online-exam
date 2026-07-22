package lsgwr.exam.controller;

import lsgwr.exam.annotation.RoleRequired;
import lsgwr.exam.enums.RoleEnum;
import lsgwr.exam.exception.ExamException;
import lsgwr.exam.qo.DownloadQo;
import lsgwr.exam.qo.UploadModel;
import lsgwr.exam.qo.UploadModel2;
import lsgwr.exam.utils.FileTransUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

/***********************************************************
 * @note      : 文件上传下载的接口,由于Swagger的问题导致在SwaggerUI
 *              里测试不成功，实际上前端是完全没有问题的
 * @author    :  梁山广
 *  * 为了支持Ajax请求和响应，最简单的解决方案返回一个ResponseEntity。
 *  * 以下示例演示了上传文件的三种可能方式：
 *  * 1. 单文件上传 - `MultipartFile`
 *  * 2. 多文件上传 - `MultipartFile []`
 *  * 3. 将文件上传到模型 - `@ModelAttribute`
 * @version   : V1.0 at 2018/7/16 20:43
 ***********************************************************/
@RestController
@Api(tags = "Upload And Download APIs")
@RequestMapping("/api/file")
@Slf4j
public class UploadDownloadController {

    /**
     * P0-6 修复 [A-03]：文件上传下载根目录，下载接口仅允许访问此目录下的文件
     */
    @Value("${file.upload-root:/data/exam/uploads/}")
    private String uploadRoot;

    @ApiOperation("单文件上传,支持同时传入参数")
    @PostMapping("/upload/singleAndparas")
    @RoleRequired({RoleEnum.TEACHER, RoleEnum.ADMIN})
    public String uploadFileSingle(@RequestParam("dir") String dir, @RequestParam("file") MultipartFile uploadfile) {
        // P0-6 修复 [A-03]：校验上传目录，防止路径穿越
        validateUploadDir(dir);
        return FileTransUtil.uploadFile(uploadfile, dir);
    }

    @ApiOperation("单文件上传,支持同时传入参数,Model")
    @PostMapping("/upload/single/model")
    @RoleRequired({RoleEnum.TEACHER, RoleEnum.ADMIN})
    public String singleUploadFileModel(@ModelAttribute("model") UploadModel2 model) {
        validateUploadDir(model.getDir());
        return FileTransUtil.uploadFile(model.getFile(), model.getDir());
    }

    @ApiOperation("多文件上传,支持同时传入参数")
    @PostMapping("/upload/multiAndparas")
    @RoleRequired({RoleEnum.TEACHER, RoleEnum.ADMIN})
    public String uploadFileMulti(@RequestParam("dir") String dir, @RequestParam("files") MultipartFile[] uploadfiles) {
        validateUploadDir(dir);
        return FileTransUtil.uploadFiles(uploadfiles, dir);
    }

    @ApiOperation("多文件上传,支持同时传入参数")
    @PostMapping(value = "/upload/multi/model")
    @RoleRequired({RoleEnum.TEACHER, RoleEnum.ADMIN})
    public String multiUploadFileModel(@ModelAttribute(("model")) UploadModel model) {
        validateUploadDir(model.getDir());
        return FileTransUtil.uploadFiles(model.getFiles(), model.getDir());
    }

    @ApiOperation("Get下载文件")
    @GetMapping(value = "/download/get")
    public ResponseEntity<InputStreamResource> downloadFileGet(@RequestParam String filePath) throws IOException {
        // P0-6 修复 [A-03]：校验下载路径，防止路径穿越攻击（如 ../../etc/passwd）
        String safePath = safeResolveDownloadPath(filePath);
        return FileTransUtil.downloadFile(safePath);
    }

    @ApiOperation("Post下载文件")
    @PostMapping(value = "/download/post")
    public ResponseEntity<InputStreamResource> downloadFilePost(@RequestBody DownloadQo downloadQo) throws IOException {
        String safePath = safeResolveDownloadPath(downloadQo.getPath());
        return FileTransUtil.downloadFile(safePath);
    }

    // ==================== P0-6 路径校验私有方法 ====================

    /**
     * 校验上传目录：禁止包含路径穿越符 ".."，禁止绝对路径
     *
     * @param dir 用户传入的目录参数
     */
    private void validateUploadDir(String dir) {
        if (dir == null || dir.isEmpty()) {
            throw new ExamException(1, "上传目录不能为空");
        }
        // 替换反斜杠为正斜杠后规范化
        String normalized = dir.replace('\\', '/').trim();
        // 禁止路径穿越
        if (normalized.contains("..")) {
            throw new ExamException(1, "非法目录路径");
        }
        // 禁止绝对路径（以 / 或盘符开头）
        if (normalized.startsWith("/") || normalized.matches("^[A-Za-z]:.*")) {
            throw new ExamException(1, "禁止使用绝对路径");
        }
    }

    /**
     * 校验下载路径：解析后的绝对路径必须位于 uploadRoot 下
     * 同时支持相对路径（自动拼接 uploadRoot）和已包含 uploadRoot 的绝对路径
     *
     * @param filePath 用户传入的文件路径
     * @return 安全的绝对路径
     */
    private String safeResolveDownloadPath(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            throw new ExamException(1, "文件路径不能为空");
        }
        try {
            File root = new File(uploadRoot).getCanonicalFile();
            File target;
            String normalized = filePath.replace('\\', '/').trim();
            // 禁止路径穿越
            if (normalized.contains("..")) {
                throw new ExamException(1, "非法文件路径");
            }
            // 若传入的是绝对路径且已在 uploadRoot 下，直接使用
            File filePathFile = new File(normalized);
            if (filePathFile.isAbsolute()) {
                target = filePathFile.getCanonicalFile();
            } else {
                // 相对路径：拼接 uploadRoot
                target = new File(root, normalized).getCanonicalFile();
            }
            // 校验最终路径必须在 uploadRoot 下
            if (!target.getPath().startsWith(root.getPath())) {
                throw new ExamException(1, "非法文件路径");
            }
            if (!target.exists()) {
                throw new ExamException(1, "文件不存在");
            }
            return target.getAbsolutePath();
        } catch (ExamException e) {
            throw e;
        } catch (IOException e) {
            throw new ExamException(1, "文件路径解析失败");
        }
    }
}
