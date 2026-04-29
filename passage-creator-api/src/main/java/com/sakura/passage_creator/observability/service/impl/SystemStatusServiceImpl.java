package com.sakura.passage_creator.observability.service.impl;

import com.sakura.passage_creator.observability.config.ObservabilityProperties;
import com.sakura.passage_creator.observability.enums.ObservabilityStatusLevelEnum;
import com.sakura.passage_creator.observability.model.vo.DependencyStatusVO;
import com.sakura.passage_creator.observability.model.vo.JvmStatusVO;
import com.sakura.passage_creator.observability.model.vo.MetricSnapshotVO;
import com.sakura.passage_creator.observability.model.vo.OsStatusVO;
import com.sakura.passage_creator.observability.model.vo.SystemStatusVO;
import com.sakura.passage_creator.observability.service.SystemStatusService;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Service;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.software.os.FileSystem;
import oshi.software.os.OSFileStore;

import javax.sql.DataSource;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.ThreadMXBean;
import java.sql.Connection;
import java.util.Date;
import java.util.List;

/**
 * 系统状态聚合服务实现。
 *
 * @author Sakura
 */
@Service
public class SystemStatusServiceImpl implements SystemStatusService {

    /**
     * 字节单位。
     */
    private static final String UNIT_BYTES = "bytes";

    /**
     * 百分比单位。
     */
    private static final String UNIT_PERCENT = "%";

    /**
     * 数据源。
     */
    private final DataSource dataSource;

    /**
     * Redis 连接工厂。
     */
    private final RedisConnectionFactory redisConnectionFactory;

    /**
     * 可观测性配置。
     */
    private final ObservabilityProperties properties;

    /**
     * OSHI 系统信息入口。
     */
    private final SystemInfo systemInfo = new SystemInfo();

    /**
     * JVM 内存指标构造器。
     */
    private final JvmMemoryMetricBuilder jvmMemoryMetricBuilder;

    public SystemStatusServiceImpl(DataSource dataSource, RedisConnectionFactory redisConnectionFactory,
            ObservabilityProperties properties) {
        this.dataSource = dataSource;
        this.redisConnectionFactory = redisConnectionFactory;
        this.properties = properties;
        this.jvmMemoryMetricBuilder = new JvmMemoryMetricBuilder(
                properties.getMemoryWarningPercent(), properties.getMemoryCriticalPercent());
    }

    @Override
    public SystemStatusVO getSystemStatus() {
        SystemStatusVO vo = new SystemStatusVO();
        vo.setSampleTime(new Date());
        vo.setJvm(buildJvmStatus());
        vo.setOs(buildOsStatus());
        vo.setDatabase(checkDatabase());
        vo.setRedis(checkRedis());
        vo.setOverallStatus(resolveOverallStatus(vo));
        return vo;
    }

    /**
     * 构造 JVM 状态。
     */
    private JvmStatusVO buildJvmStatus() {
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
        JvmStatusVO vo = new JvmStatusVO();
        vo.setHeapMemory(jvmMemoryMetricBuilder.buildHeapMemoryMetric("堆内存", memoryBean.getHeapMemoryUsage()));
        vo.setNonHeapMemory(jvmMemoryMetricBuilder.buildNonHeapMemoryMetric("非堆内存", memoryBean.getNonHeapMemoryUsage()));
        vo.setThreadCount(threadBean.getThreadCount());
        vo.setDaemonThreadCount(threadBean.getDaemonThreadCount());
        vo.setGcCount(sumGcCount());
        vo.setGcTimeMillis(sumGcTime());
        vo.setStatus(worstStatus(vo.getHeapMemory().getStatus(), vo.getNonHeapMemory().getStatus()));
        return vo;
    }

    /**
     * 构造操作系统状态。
     */
    private OsStatusVO buildOsStatus() {
        CentralProcessor processor = systemInfo.getHardware().getProcessor();
        GlobalMemory memory = systemInfo.getHardware().getMemory();
        OsStatusVO vo = new OsStatusVO();
        vo.setSystemCpu(buildPercentMetric("系统 CPU", processor.getSystemCpuLoad(1000) * 100,
                properties.getCpuWarningPercent(), properties.getCpuCriticalPercent()));
        vo.setProcessCpu(buildProcessCpuMetric(systemInfo.getOperatingSystem().getProcess(0).getProcessCpuLoadCumulative(),
                processor.getLogicalProcessorCount()));
        vo.setMemory(buildCapacityMetric("系统内存", memory.getTotal() - memory.getAvailable(), memory.getTotal(),
                properties.getMemoryWarningPercent(), properties.getMemoryCriticalPercent()));
        vo.setDisk(buildDiskMetric());
        vo.setStatus(worstStatus(vo.getSystemCpu().getStatus(), vo.getProcessCpu().getStatus(),
                vo.getMemory().getStatus(), vo.getDisk().getStatus()));
        return vo;
    }

    /**
     * 构造数据库状态。
     */
    private DependencyStatusVO checkDatabase() {
        long start = System.currentTimeMillis();
        DependencyStatusVO vo = new DependencyStatusVO();
        vo.setName("数据库");
        try (Connection connection = dataSource.getConnection()) {
            vo.setStatus(connection.isValid(2)
                    ? ObservabilityStatusLevelEnum.UP.getValue()
                    : ObservabilityStatusLevelEnum.DOWN.getValue());
            vo.setMessage("数据库连接正常");
            fillHikariMetrics(vo);
        } catch (Exception e) {
            vo.setStatus(ObservabilityStatusLevelEnum.DOWN.getValue());
            vo.setMessage(e.getClass().getSimpleName() + ": " + e.getMessage());
        }
        vo.setLatencyMillis(System.currentTimeMillis() - start);
        return vo;
    }

    /**
     * 构造 Redis 状态。
     */
    private DependencyStatusVO checkRedis() {
        long start = System.currentTimeMillis();
        DependencyStatusVO vo = new DependencyStatusVO();
        vo.setName("Redis");
        try (RedisConnection connection = redisConnectionFactory.getConnection()) {
            String pong = connection.ping();
            vo.setStatus("PONG".equalsIgnoreCase(pong)
                    ? ObservabilityStatusLevelEnum.UP.getValue()
                    : ObservabilityStatusLevelEnum.DEGRADED.getValue());
            vo.setMessage("Redis 响应：" + pong);
        } catch (Exception e) {
            vo.setStatus(ObservabilityStatusLevelEnum.DOWN.getValue());
            vo.setMessage(e.getClass().getSimpleName() + ": " + e.getMessage());
        }
        vo.setLatencyMillis(System.currentTimeMillis() - start);
        return vo;
    }

    /**
     * 构造磁盘指标。
     */
    private MetricSnapshotVO buildDiskMetric() {
        FileSystem fileSystem = systemInfo.getOperatingSystem().getFileSystem();
        List<OSFileStore> stores = fileSystem.getFileStores();
        long total = stores.stream().mapToLong(OSFileStore::getTotalSpace).sum();
        long usable = stores.stream().mapToLong(OSFileStore::getUsableSpace).sum();
        return buildCapacityMetric("磁盘", total - usable, total,
                properties.getDiskWarningPercent(), properties.getDiskCriticalPercent());
    }

    /**
     * 构造容量指标。
     */
    private MetricSnapshotVO buildCapacityMetric(String name, long used, long total, double warning, double critical) {
        double usagePercent = total <= 0 ? 0 : used * 100D / total;
        MetricSnapshotVO vo = new MetricSnapshotVO();
        vo.setName(name);
        vo.setValue(used);
        vo.setUnit(UNIT_BYTES);
        vo.setUsed(used);
        vo.setTotal(total);
        vo.setUsagePercent(usagePercent);
        vo.setStatus(ObservabilityStatusLevelEnum.fromUsage(usagePercent, warning, critical).getValue());
        return vo;
    }

    /**
     * 构造百分比指标。
     */
    private MetricSnapshotVO buildPercentMetric(String name, double value, double warning, double critical) {
        double safeValue = Double.isNaN(value) || value < 0 ? 0 : value;
        MetricSnapshotVO vo = new MetricSnapshotVO();
        vo.setName(name);
        vo.setValue(safeValue);
        vo.setUnit(UNIT_PERCENT);
        vo.setUsagePercent(safeValue);
        vo.setStatus(ObservabilityStatusLevelEnum.fromUsage(safeValue, warning, critical).getValue());
        return vo;
    }

    /**
     * 构造进程 CPU 指标。
     */
    MetricSnapshotVO buildProcessCpuMetric(double processCpuLoad, int logicalProcessorCount) {
        // OSHI 在部分平台会返回按逻辑核心累计后的进程负载，需换算为整机 0-100 百分比。
        int safeProcessorCount = Math.max(logicalProcessorCount, 1);
        double normalizedPercent = processCpuLoad * 100 / safeProcessorCount;
        return buildPercentMetric("进程 CPU", Math.min(normalizedPercent, 100D),
                properties.getCpuWarningPercent(), properties.getCpuCriticalPercent());
    }

    /**
     * 汇总 GC 次数。
     */
    private long sumGcCount() {
        return ManagementFactory.getGarbageCollectorMXBeans().stream()
                .mapToLong(GarbageCollectorMXBean::getCollectionCount)
                .filter(value -> value > 0)
                .sum();
    }

    /**
     * 汇总 GC 耗时。
     */
    private long sumGcTime() {
        return ManagementFactory.getGarbageCollectorMXBeans().stream()
                .mapToLong(GarbageCollectorMXBean::getCollectionTime)
                .filter(value -> value > 0)
                .sum();
    }

    /**
     * 填充 Hikari 连接池指标。
     */
    private void fillHikariMetrics(DependencyStatusVO vo) {
        if (!(dataSource instanceof HikariDataSource hikariDataSource)) {
            return;
        }
        HikariPoolMXBean poolBean = hikariDataSource.getHikariPoolMXBean();
        if (poolBean == null) {
            return;
        }
        vo.setActiveConnections(poolBean.getActiveConnections());
        vo.setIdleConnections(poolBean.getIdleConnections());
        vo.setTotalConnections(poolBean.getTotalConnections());
    }

    /**
     * 计算综合状态。
     */
    private String resolveOverallStatus(SystemStatusVO vo) {
        return worstStatus(vo.getJvm().getStatus(), vo.getOs().getStatus(),
                vo.getDatabase().getStatus(), vo.getRedis().getStatus());
    }

    /**
     * 选择最差状态。
     */
    private String worstStatus(String... statuses) {
        for (String status : statuses) {
            if (ObservabilityStatusLevelEnum.DOWN.getValue().equals(status)) {
                return ObservabilityStatusLevelEnum.DOWN.getValue();
            }
        }
        for (String status : statuses) {
            if (ObservabilityStatusLevelEnum.DEGRADED.getValue().equals(status)) {
                return ObservabilityStatusLevelEnum.DEGRADED.getValue();
            }
        }
        return ObservabilityStatusLevelEnum.UP.getValue();
    }
}
