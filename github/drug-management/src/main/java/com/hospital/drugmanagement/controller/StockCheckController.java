package com.hospital.drugmanagement.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hospital.drugmanagement.common.anno.RequireRole;
import com.hospital.drugmanagement.entity.DrugInfo;
import com.hospital.drugmanagement.entity.DrugStock;
import com.hospital.drugmanagement.entity.StockCheck;
import com.hospital.drugmanagement.entity.StockCheckItem;
import com.hospital.drugmanagement.entity.WarehouseInfo;
import com.hospital.drugmanagement.entity.SysUser;
import com.hospital.drugmanagement.mapper.DrugInfoMapper;
import com.hospital.drugmanagement.mapper.DrugStockMapper;
import com.hospital.drugmanagement.mapper.WarehouseInfoMapper;
import com.hospital.drugmanagement.mapper.SysUserMapper;
import com.hospital.drugmanagement.service.IStockCheckItemService;
import com.hospital.drugmanagement.service.IStockCheckService;
import com.hospital.drugmanagement.service.StockWarningService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 库存盘点接口：创建盘点单、录入实盘数量、计算盈亏并调整库存。
 */
@RestController
@RequestMapping("/api/stock-check")
@CrossOrigin(origins = "*")
@RequireRole({"ADMIN", "WAREHOUSE"})
public class StockCheckController {

    @Autowired
    private IStockCheckService stockCheckService;

    @Autowired
    private IStockCheckItemService stockCheckItemService;

    @Autowired
    private DrugStockMapper drugStockMapper;

    @Autowired
    private DrugInfoMapper drugInfoMapper;

    @Autowired
    private WarehouseInfoMapper warehouseInfoMapper;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private StockWarningService stockWarningService;

    /** 分页查询盘点单列表，支持仓库、状态筛选。 */
    @GetMapping("/list")
    public Map<String, Object> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) Integer status
    ) {
        Map<String, Object> result = new HashMap<>();
        try {
            LambdaQueryWrapper<StockCheck> queryWrapper = new LambdaQueryWrapper<>();
            if (warehouseId != null) {
                queryWrapper.eq(StockCheck::getWarehouseId, warehouseId);
            }
            if (status != null) {
                queryWrapper.eq(StockCheck::getStatus, status);
            }
            queryWrapper.orderByDesc(StockCheck::getCreateTime);

            Page<StockCheck> pageResult = stockCheckService.page(new Page<>(page, size), queryWrapper);

            List<Map<String, Object>> records = new ArrayList<>();
            for (StockCheck sc : pageResult.getRecords()) {
                Map<String, Object> map = new HashMap<>();
                map.put("checkId", sc.getCheckId());
                map.put("checkNo", sc.getCheckNo());
                map.put("warehouseId", sc.getWarehouseId());
                map.put("checkTime", sc.getCheckTime());
                map.put("status", sc.getStatus());
                map.put("remark", sc.getRemark());
                map.put("checkUserId", sc.getCheckUserId());
                map.put("createTime", sc.getCreateTime());

                if (sc.getWarehouseId() != null) {
                    WarehouseInfo wh = warehouseInfoMapper.selectById(sc.getWarehouseId());
                    if (wh != null) {
                        map.put("warehouseName", wh.getWarehouseName());
                    }
                }

                if (sc.getCheckUserId() != null) {
                    SysUser user = sysUserMapper.selectById(sc.getCheckUserId());
                    if (user != null) {
                        map.put("operatorName", user.getRealName());
                    }
                }

                records.add(map);
            }

            result.put("code", 200);
            result.put("msg", "success");
            result.put("data", records);
            result.put("total", pageResult.getTotal());
        } catch (Exception e) {
            result.put("code", 500);
            result.put("msg", "查询失败：" + e.getMessage());
            result.put("data", null);
            result.put("total", 0);
        }
        return result;
    }

    /** 查询盘点单详情及明细（含药品名称、批号、盈亏数量）。 */
    @GetMapping("/{id}")
    public Map<String, Object> getById(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            StockCheck stockCheck = stockCheckService.getById(id);
            if (stockCheck == null) {
                result.put("code", 404);
                result.put("msg", "盘点单不存在");
                result.put("data", null);
                return result;
            }

            Map<String, Object> data = new HashMap<>();
            data.put("checkId", stockCheck.getCheckId());
            data.put("checkNo", stockCheck.getCheckNo());
            data.put("warehouseId", stockCheck.getWarehouseId());
            data.put("checkTime", stockCheck.getCheckTime());
            data.put("status", stockCheck.getStatus());
            data.put("remark", stockCheck.getRemark());
            data.put("checkUserId", stockCheck.getCheckUserId());
            data.put("createTime", stockCheck.getCreateTime());

            if (stockCheck.getWarehouseId() != null) {
                WarehouseInfo wh = warehouseInfoMapper.selectById(stockCheck.getWarehouseId());
                if (wh != null) {
                    data.put("warehouseName", wh.getWarehouseName());
                }
            }

            LambdaQueryWrapper<StockCheckItem> itemWrapper = new LambdaQueryWrapper<>();
            itemWrapper.eq(StockCheckItem::getCheckId, id);
            List<StockCheckItem> items = stockCheckItemService.list(itemWrapper);

            List<Map<String, Object>> itemList = new ArrayList<>();
            for (StockCheckItem item : items) {
                Map<String, Object> itemMap = new HashMap<>();
                itemMap.put("itemId", item.getItemId());
                itemMap.put("checkId", item.getCheckId());
                itemMap.put("drugId", item.getDrugId());
                itemMap.put("batchNo", item.getBatchNo());
                itemMap.put("systemNum", item.getSystemNum());
                itemMap.put("actualNum", item.getActualNum());
                itemMap.put("diffNum", item.getDiffNum());
                itemMap.put("handleWay", item.getHandleWay());
                itemMap.put("handleRemark", item.getHandleRemark());

                DrugInfo drugInfo = drugInfoMapper.selectById(item.getDrugId());
                if (drugInfo != null) {
                    itemMap.put("drugName", drugInfo.getDrugName());
                    itemMap.put("spec", drugInfo.getSpec());
                    itemMap.put("unit", drugInfo.getUnit());
                }

                itemList.add(itemMap);
            }
            data.put("items", itemList);

            result.put("code", 200);
            result.put("msg", "success");
            result.put("data", data);
        } catch (Exception e) {
            result.put("code", 500);
            result.put("msg", "查询失败：" + e.getMessage());
            result.put("data", null);
        }
        return result;
    }

    /** 新建盘点单：按仓库当前库存自动生成明细（含批号、系统数量）。 */
    @PostMapping
    public Map<String, Object> save(
            @RequestAttribute("currentUserId") Long currentUserId,
            @RequestBody Map<String, Object> params) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long warehouseId = params.get("warehouseId") != null ? Long.valueOf(params.get("warehouseId").toString()) : null;
            String remark = params.get("remark") != null ? params.get("remark").toString() : null;

            StockCheck stockCheck = new StockCheck();
            stockCheck.setCheckNo("PD" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));
            stockCheck.setWarehouseId(warehouseId);
            stockCheck.setCheckTime(LocalDateTime.now());
            stockCheck.setStatus(0);
            stockCheck.setRemark(remark);
            stockCheck.setCheckUserId(currentUserId);

            stockCheckService.save(stockCheck);

            LambdaQueryWrapper<DrugStock> stockWrapper = new LambdaQueryWrapper<>();
            if (warehouseId != null) {
                stockWrapper.eq(DrugStock::getWarehouseId, warehouseId);
            }
            List<DrugStock> stockList = drugStockMapper.selectList(stockWrapper);

            List<StockCheckItem> items = new ArrayList<>();
            for (DrugStock stock : stockList) {
                if (stock.getStockNum() == null || stock.getStockNum() <= 0) {
                    continue;
                }
                StockCheckItem item = new StockCheckItem();
                item.setCheckId(stockCheck.getCheckId());
                item.setDrugId(stock.getDrugId());
                item.setBatchNo(stock.getBatchNo());
                item.setSystemNum(stock.getStockNum());
                item.setActualNum(stock.getStockNum());
                item.setDiffNum(0);
                items.add(item);
            }
            if (!items.isEmpty()) {
                stockCheckItemService.saveBatch(items);
            }

            result.put("code", 200);
            result.put("msg", "创建盘点单成功");
            result.put("data", stockCheck.getCheckId());
        } catch (Exception e) {
            result.put("code", 500);
            result.put("msg", "创建盘点单失败：" + e.getMessage());
            result.put("data", null);
        }
        return result;
    }

    /**
     * 完成盘点：保存实盘数量，按批次调整库存，触发预警检查。
     * <p>
     * 请求体需包含 checkId 及 items（itemId、actualNum、handleWay）。
     */
    @PutMapping("/complete")
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> complete(@RequestBody Map<String, Object> params) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long checkId = Long.valueOf(params.get("checkId").toString());
            StockCheck stockCheck = stockCheckService.getById(checkId);
            if (stockCheck == null) {
                result.put("code", 404);
                result.put("msg", "盘点单不存在");
                result.put("data", null);
                return result;
            }
            if (stockCheck.getStatus() != 0) {
                result.put("code", 400);
                result.put("msg", "只有盘点中的单据才能完成");
                result.put("data", null);
                return result;
            }

            List<Map<String, Object>> itemList = (List<Map<String, Object>>) params.get("items");
            if (itemList != null) {
                for (Map<String, Object> itemData : itemList) {
                    Long itemId = Long.valueOf(itemData.get("itemId").toString());
                    StockCheckItem item = stockCheckItemService.getById(itemId);
                    if (item == null) {
                        continue;
                    }
                    if (itemData.get("actualNum") != null) {
                        item.setActualNum(Integer.valueOf(itemData.get("actualNum").toString()));
                    }
                    int systemNum = item.getSystemNum() != null ? item.getSystemNum() : 0;
                    int actualNum = item.getActualNum() != null ? item.getActualNum() : systemNum;
                    item.setDiffNum(actualNum - systemNum);
                    if (itemData.get("handleWay") != null) {
                        item.setHandleWay(itemData.get("handleWay").toString());
                    }
                    if (itemData.get("handleRemark") != null) {
                        item.setHandleRemark(itemData.get("handleRemark").toString());
                    }
                    stockCheckItemService.updateById(item);

                    if (item.getDiffNum() != 0 && item.getDrugId() != null) {
                        applyStockAdjustment(stockCheck, item);
                        stockWarningService.checkAndCreateWarning(item.getDrugId(), stockCheck.getWarehouseId());
                    }
                }
            } else {
                // 未提交明细时，按已有明细中的差异同步库存
                LambdaQueryWrapper<StockCheckItem> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(StockCheckItem::getCheckId, checkId);
                List<StockCheckItem> existingItems = stockCheckItemService.list(wrapper);
                for (StockCheckItem item : existingItems) {
                    if (item.getDiffNum() != null && item.getDiffNum() != 0 && item.getDrugId() != null) {
                        applyStockAdjustment(stockCheck, item);
                        stockWarningService.checkAndCreateWarning(item.getDrugId(), stockCheck.getWarehouseId());
                    }
                }
            }

            stockCheck.setStatus(1);
            stockCheckService.updateById(stockCheck);

            result.put("code", 200);
            result.put("msg", "盘点完成");
            result.put("data", null);
        } catch (IllegalArgumentException e) {
            result.put("code", 400);
            result.put("msg", e.getMessage());
            result.put("data", null);
        } catch (Exception e) {
            result.put("code", 500);
            result.put("msg", "完成盘点失败：" + e.getMessage());
            result.put("data", null);
        }
        return result;
    }

    /**
     * 按盘点明细将对应批次库存调整为实盘数量。
     */
    private void applyStockAdjustment(StockCheck stockCheck, StockCheckItem item) {
        LambdaQueryWrapper<DrugStock> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DrugStock::getDrugId, item.getDrugId())
                .eq(DrugStock::getWarehouseId, stockCheck.getWarehouseId());
        if (StringUtils.hasText(item.getBatchNo())) {
            wrapper.eq(DrugStock::getBatchNo, item.getBatchNo());
        } else if (item.getSystemNum() != null) {
            wrapper.eq(DrugStock::getStockNum, item.getSystemNum());
        }

        DrugStock stock = drugStockMapper.selectOne(wrapper, false);
        if (stock == null) {
            throw new IllegalArgumentException("未找到药品【" + item.getDrugId() + "】对应的库存批次，无法调整");
        }

        int targetQty = item.getActualNum() != null ? item.getActualNum() : 0;
        if (targetQty < 0) {
            throw new IllegalArgumentException("实盘数量不能为负数");
        }

        stock.setStockNum(targetQty);
        stock.setUpdateTime(LocalDateTime.now());
        drugStockMapper.updateById(stock);
    }

    /** 取消盘点单（仅「盘点中」状态可取消）。 */
    @PutMapping("/cancel/{id}")
    public Map<String, Object> cancel(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            StockCheck stockCheck = stockCheckService.getById(id);
            if (stockCheck == null) {
                result.put("code", 404);
                result.put("msg", "盘点单不存在");
                result.put("data", null);
                return result;
            }
            if (stockCheck.getStatus() != 0) {
                result.put("code", 400);
                result.put("msg", "只有盘点中的单据才能取消");
                result.put("data", null);
                return result;
            }

            stockCheck.setStatus(2);
            stockCheckService.updateById(stockCheck);

            result.put("code", 200);
            result.put("msg", "取消盘点成功");
            result.put("data", null);
        } catch (Exception e) {
            result.put("code", 500);
            result.put("msg", "取消盘点失败：" + e.getMessage());
            result.put("data", null);
        }
        return result;
    }

    /** 删除盘点单及明细（已完成的不可删除）。 */
    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            StockCheck stockCheck = stockCheckService.getById(id);
            if (stockCheck == null) {
                result.put("code", 404);
                result.put("msg", "盘点单不存在");
                result.put("data", null);
                return result;
            }
            if (stockCheck.getStatus() == 1) {
                result.put("code", 400);
                result.put("msg", "已完成的盘点单不能删除");
                result.put("data", null);
                return result;
            }

            LambdaQueryWrapper<StockCheckItem> itemWrapper = new LambdaQueryWrapper<>();
            itemWrapper.eq(StockCheckItem::getCheckId, id);
            stockCheckItemService.remove(itemWrapper);

            stockCheckService.removeById(id);

            result.put("code", 200);
            result.put("msg", "删除成功");
            result.put("data", null);
        } catch (Exception e) {
            result.put("code", 500);
            result.put("msg", "删除失败：" + e.getMessage());
            result.put("data", null);
        }
        return result;
    }
}
