package com.puyin.cn.service.serviceIpml;

import com.puyin.cn.BO.SubmitOrderBo;
import com.puyin.cn.dao.ProcurementDao;
import com.puyin.cn.dao.SalesDao;
import com.puyin.cn.entity.*;
import com.puyin.cn.service.SalesService;
import com.puyin.cn.util.MyStringUtil;
import com.puyin.cn.vo.SalesVo;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.ArrayList;
import java.util.List;

/**
 * 描述：
 *
 * @author PuYinsheng
 * @date 2020/3/30
 **/
@Service
public class SalesServiceImpl implements SalesService {
    @Autowired
    private SalesDao salesDao;
    @Autowired
    private ProcurementDao procurementDao;

    /**
     * 查询所有商品
     * @return
     */
    @Override
    public List<SalesVo> findAllProduct() {
        List<SalesPO> allProduct = salesDao.findAllProduct();
        ArrayList<SalesVo> list = new ArrayList<>();
        for (SalesPO salesPO : allProduct) {
            SalesVo salesVo = new SalesVo();
            BeanUtils.copyProperties(salesPO,salesVo);
            list.add(salesVo);
        }
        return list;
    }

    /**
     * 查询所有商品种类
     * @return
     */
    @Override
    public List<CategroyPo> finfAllCategroy() {
        return salesDao.findAllCateGroy();
    }

    @Override
    public int intsertTemporary(TemporaryPo temporaryPo) {
        return salesDao.insertTemporary(temporaryPo);
    }

    @Override
    public List<TemporaryPo> selectTemporary(@PathVariable("accountName") String accountName) {
        List<TemporaryPo> temporaryPos = salesDao.selectTemporary(accountName);
        salesDao.deleteTemporary(accountName);
        return temporaryPos;
    }
    /**
     * 查询销售部提交订单货物库存
     * @param submitOrderBo
     * @return
     */
    @Override
    public int findProductCount(SubmitOrderBo submitOrderBo) {
        int result=0;
        List<TemporaryPo> productList = submitOrderBo.getProductList();
        //生成订单编号
        String orderNo="EPSS"+MyStringUtil.getTimeToString();
        //1.提取提交货物id集合
        ArrayList<Integer> ids = new ArrayList<>();
        for (TemporaryPo temporaryPo : productList) {
            ids.add(temporaryPo.getProductId());
        }
        //1.1提取提交货物id和count集合
        ArrayList<CommodityStocksPo> commodityStocksPos = new ArrayList<>();
        for (TemporaryPo temporaryPo : productList) {
            CommodityStocksPo commodityStocksPo = new CommodityStocksPo();
            commodityStocksPo.setId(temporaryPo.getProductId());
            commodityStocksPo.setProductCount(temporaryPo.getCount());
            commodityStocksPos.add(commodityStocksPo);
        }
        //2.调用salesDao根据提交货物id集合,查询出货物库存和id并进行比较
        List<CommodityStocksPo> productOrderCountlist = salesDao.findProductCount(ids);
        //缺货商品集合(id.count集合)
        ArrayList<CommodityStocksPo> productStockoutList = new ArrayList<>();
        //不缺货商品集合(id.count集合)
        ArrayList<CommodityStocksPo> productNoStockoutList = new ArrayList<>();
        for (int i = 0; i < commodityStocksPos.size(); i++) {
            for (int j = 0; j < productOrderCountlist.size(); j++) {
                //提取商品集合
                CommodityStocksPo orderIdCount = commodityStocksPos.get(i);
                CommodityStocksPo productIdCount = productOrderCountlist.get(j);
                //转化数量类型
                Double orderCout = MyStringUtil.SubStringNumber(orderIdCount.getProductCount());
                Double productCout = MyStringUtil.SubStringNumber(productIdCount.getProductCount());
                if(orderIdCount.getId().equals(productIdCount.getId()) && orderCout>productCout){
                    //缺货商品
                    //缺货数量
                    double StockoutCount = orderCout - productCout;
                    //缺货id
                    Integer StockoutId = orderIdCount.getId();
                    //缺货商品单位
                    String StockoutUnit = MyStringUtil.subStringStr(productIdCount.getProductCount());
                    CommodityStocksPo productStockout = new CommodityStocksPo();
                    productStockout.setId(StockoutId);
                    productStockout.setProductCount(StockoutCount+StockoutUnit);
                    productStockoutList.add(productStockout);
                }else if(orderIdCount.getId().equals(productIdCount.getId()) && orderCout<=productCout){
                    //不缺货商品添加单位
                    String StockoutUnit = MyStringUtil.subStringStr(productIdCount.getProductCount());
                    String productCount = orderIdCount.getProductCount();
                    orderIdCount.setProductCount(productCount+StockoutUnit);
                    productNoStockoutList.add(orderIdCount);
                }
            }
        }
        //3.将缺货商品生成采购单
        if(productStockoutList.size()!=0){
            //下达采购单人姓名
            PurchaseOrderPo purchaseOrderPo = new PurchaseOrderPo();
            purchaseOrderPo.setPurchaseSellName(submitOrderBo.getProductList().get(0).getAccountName());
            //生成采购单号
            String purchaseOrderNo = "EPS" + MyStringUtil.getTimeToString();
            purchaseOrderPo.setPurchaseOrderNo(purchaseOrderNo);
            purchaseOrderPo.setOrderNo(orderNo);
            procurementDao.insertPurchaseOrder(purchaseOrderPo);
            Integer purchaseOrderId = procurementDao.selectPurchaseId(purchaseOrderNo);
            ArrayList<purchaseOrderProductPo> purchaseOrderProductPos = new ArrayList<>();
            for (CommodityStocksPo commodityStocksPo : productStockoutList) {
                purchaseOrderProductPo purchaseOrderProductPo = new purchaseOrderProductPo();
                String productName = salesDao.findProductNameById(commodityStocksPo.getId());
                purchaseOrderProductPo.setPurchaseOrderId(purchaseOrderId);
                purchaseOrderProductPo.setProductName(productName);
                purchaseOrderProductPo.setProductCount(commodityStocksPo.getProductCount());
                purchaseOrderProductPos.add(purchaseOrderProductPo);
            }
            procurementDao.insetPurchaseOrderProduct(purchaseOrderProductPos);
        }
        //4.将不缺货商品生成商品出库单
        if(productNoStockoutList.size()!=0){
            //4.1生成出库单编号
            String warehouseNo = "EPSWO"+MyStringUtil.getTimeToString();
            procurementDao.AddOutboundOrder(warehouseNo);
            Long warehouseId = procurementDao.findNOBywarehouseId(warehouseNo);
            for (CommodityStocksPo commodityStocksPo : productNoStockoutList) {
                String productName = salesDao.findProductNameById(commodityStocksPo.getId());
                String productCount = commodityStocksPo.getProductCount();
                procurementDao.addOutboundOrderInfo(productName,productCount,warehouseId);
            }
        }
        //5.生成订单,订单状态分为有缺货商品和无缺货商品两种状态
        if(productStockoutList.size()==0){
            //不缺货状态
            
        }else {
            //缺货状态
        }
        if (productStockoutList.size()>=1){
            result=1;
        }
        return result;
    }
}
