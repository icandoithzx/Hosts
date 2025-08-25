package com.example.demo.service;

/**
 * 同步服务接口
 * 定义外部数据同步的标准规范
 */
public interface SyncService {
    
    /**
     * 检查是否需要同步
     * @return true需要同步，false不需要同步
     */
    boolean needSync();
    
    /**
     * 执行同步操作
     * @return true同步成功，false同步失败
     */
    boolean executeSync();
    
    /**
     * 获取同步服务名称
     * @return 服务名称
     */
    String getServiceName();
    
    /**
     * 定时同步任务
     */
    void scheduledSync();
    
    /**
     * 手动触发同步
     */
    void manualSync();
    
    /**
     * 应用启动时的初始化同步检查
     */
    void initializeSync();
}