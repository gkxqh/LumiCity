package com.ccb.lighting.module.video.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ccb.lighting.common.BusinessException;
import com.ccb.lighting.common.ResultCode;
import com.ccb.lighting.module.video.dto.VideoCameraQueryDTO;
import com.ccb.lighting.module.video.entity.VideoCamera;
import com.ccb.lighting.module.video.mapper.VideoCameraMapper;
import com.ccb.lighting.module.video.service.VideoCameraService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GradientPaint;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 视频摄像头 Service 实现类
 *
 * <p>简单 CRUD，按创建时间倒序排列；并提供后端实时绘制抓拍图（RTSP 等无法直连时的演示兜底）。</p>
 */
@Service
@RequiredArgsConstructor
public class VideoCameraServiceImpl implements VideoCameraService {

    /** 摄像头 Mapper，构造器注入 */
    private final VideoCameraMapper videoCameraMapper;

    /**
     * 根据 id 查询摄像头
     */
    @Override
    public VideoCamera getById(Long id) {
        VideoCamera camera = videoCameraMapper.selectById(id);
        if (camera == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND);
        }
        return camera;
    }

    @Override
    public IPage<VideoCamera> pageListByQuery(VideoCameraQueryDTO query) {
        LambdaQueryWrapper<VideoCamera> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(VideoCamera::getCreateTime);
        return videoCameraMapper.selectVideoCameraPage(
                new Page<>(query.getCurrent(), query.getSize()),
                query
        );

    }

    /**
     * 新增摄像头
     */
    @Override
    public void add(VideoCamera camera) {
        // 默认状态为离线，待设备上报心跳后变在线
        if (camera.getStatus() == null) {
            camera.setStatus(0);
        }
        // 默认不支持云台
        if (camera.getPtzEnable() == null) {
            camera.setPtzEnable(0);
        }
        videoCameraMapper.insert(camera);
    }

    /**
     * 修改摄像头
     */
    @Override
    public void update(VideoCamera camera) {
        if (videoCameraMapper.selectById(camera.getId()) == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND);
        }
        videoCameraMapper.updateById(camera);
    }

    /**
     * 删除摄像头
     */
    @Override
    public void delete(Long id) {
        videoCameraMapper.deleteById(id);
    }

    /**
     * 后端实时绘制监控风格抓拍图（演示 / 兜底）。
     *
     * <p>用于浏览器无法直连的 RTSP 流：以 id + 秒级时间作随机种子，
     * 使不同摄像头画面稳定且每秒略有变化，前端以轮询方式模拟实时画面。</p>
     *
     * @param id 摄像头 ID
     * @return JPEG 图片字节
     */
    @Override
    public byte[] getSnapshot(Long id) {
        VideoCamera camera = getById(id);
        final int w = 640, h = 360;
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 夜空背景渐变
        g.setPaint(new GradientPaint(0, 0, new Color(18, 24, 38), 0, h, new Color(8, 10, 18)));
        g.fillRect(0, 0, w, h);

        // 马路
        g.setColor(new Color(40, 42, 48));
        g.fillPolygon(new int[]{120, 520, 600, 40}, new int[]{200, 200, 360, 360}, 4);
        // 车道分隔线
        g.setColor(new Color(220, 200, 90));
        for (int i = 0; i < 9; i++) {
            g.fillRect(150 + i * 40, 280, 18, 4);
        }

        // 车辆（车身体 + 红尾灯 + 白头灯）
        for (int i = 0; i < 3; i++) {
            int cx = 150 + 170;
            int cy = 232 + 45;
            g.setColor(new Color(28, 30, 36));
            g.fillRect(cx - 6, cy - 14, 36, 20);
            g.setColor(new Color(255, 80, 60));
            g.fillOval(cx, cy, 10, 6);
            g.setColor(new Color(230, 230, 230));
            g.fillOval(cx + 22, cy, 8, 6);
        }
        // 行人
        g.setColor(new Color(120, 130, 140));
        for (int i = 0; i < 2; i++) {
            int px = 80 + 240;
            int py = 305 + 17;
            g.fillRect(px, py - 18, 6, 18);
            g.fillOval(px + 3, py - 24, 6, 6);
        }

        // 中心准星
        g.setColor(new Color(255, 255, 255, 130));
        g.drawLine(w / 2 - 22, h / 2, w / 2 + 22, h / 2);
        g.drawLine(w / 2, h / 2 - 22, w / 2, h / 2 + 22);

        // 左上角信息
        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 16));
        g.drawString(trim(camera.getCameraName(), 22), 14, 26);
        g.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g.drawString("STATUS: " + statusText(camera.getStatus()), 14, 46);

        // 左下角 REC + 时间
        g.setColor(new Color(245, 108, 108));
        g.fillOval(16, h - 32, 10, 10);
        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.PLAIN, 13));
        g.drawString("REC " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()), 32, h - 22);

        // 分辨率角标
        String res = camera.getResolution() == null ? "1280x720" : camera.getResolution();
        g.drawString(res, w - 92, h - 22);

        g.dispose();

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(img, "jpg", baos);
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("抓拍图生成失败: " + e.getMessage(), e);
        }
    }

    private String statusText(Integer s) {
        if (s == null) return "UNKNOWN";
        if (s == 1) return "ONLINE";
        if (s == 2) return "FAULT";
        return "OFFLINE";
    }

    private String trim(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max) + "…" : s;
    }
}
