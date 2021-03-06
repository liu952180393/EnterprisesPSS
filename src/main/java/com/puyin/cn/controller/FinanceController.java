package com.puyin.cn.controller;

import com.puyin.cn.entity.FinancePo;
import com.puyin.cn.service.FinanceService;
import com.puyin.cn.vo.PaymentOrderInfoVo;
import org.hibernate.validator.constraints.EAN;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 描述：财务部
 *
 * @author PuYinsheng
 * @date 2020/4/14
 **/
@RestController
public class FinanceController {
    @Autowired
    private FinanceService financeService;

    /**
     * 查询所有代付款来料付款单
     * @return
     */
    @RequestMapping("findAllPaymentOrder")
    public List<FinancePo> findAllPaymentOrder(){
        return financeService.findAllPaymentOrder();
    }

    /**
     * 来料付款单完成付款
     * @param id
     * @return
     */
    @RequestMapping("finishPaymentOrder")
    public int finishPaymentOrder(Long id){
       return financeService.finishPaymentOrder(id);
    }
    /**
     * 采购付款单详情页
     * @param orderId
     * @return
     */
    @RequestMapping("findPurchaseProductList")
    public List<PaymentOrderInfoVo> findPurchaseProductList(Long orderId){
        return financeService.findPurchaseProductList(orderId);
    }
}
