# PayDemo
一个测试demo 跳转到钱包APP 调用钱包支付
首先demo请求获取订单信息的接口 (dcpayCore/payBills/prepay) 拿到订单信息之后
携带订单信息跳转到钱包APP的预支付界面(PrepayActivity) 订单携带的信息 会在当前页面展示
然后点击下一步跳转到支付界面(PayActivity)调用接口 (/dcpayCore/payBills/pay) 即可
 
