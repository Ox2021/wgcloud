package com.wgcloud.mapper;

import com.wgcloud.entity.HostInfo;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * @ClassName:HostInfoDao.java
 * @Description: ，暂未用
 */
@Repository
public interface HostInfoMapper {

    public List<HostInfo> selectAllByParams(Map<String, Object> map) throws Exception;

    public List<HostInfo> selectByParams(Map<String, Object> params) throws Exception;

    public HostInfo selectById(String id) throws Exception;

    public void save(HostInfo HostInfo) throws Exception;

    public int deleteById(String[] id) throws Exception;

    public int deleteByIp(String[] ip) throws Exception;

    public int updateById(HostInfo HostInfo) throws Exception;
}
