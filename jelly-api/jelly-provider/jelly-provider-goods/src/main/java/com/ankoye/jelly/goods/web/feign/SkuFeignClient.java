package com.ankoye.jelly.goods.web.feign;

import com.ankoye.jelly.base.result.Wrapper;
import com.ankoye.jelly.base.result.Wrappers;
import com.ankoye.jelly.goods.domain.Sku;
import com.ankoye.jelly.goods.feign.SkuFeign;
import com.ankoye.jelly.goods.reference.SkuReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * @author ankoye@qq.com
 */
@RestController
@RequestMapping("/sku")
public class SkuFeignClient implements SkuFeign {
    @Autowired
    private SkuReference skuReference;

    @Override
    public Wrapper<Sku> getSkuById(@PathVariable String id) {
        Sku sku = skuReference.selectById(id);
        return Wrappers.success(sku);
    }

    @Override
    public Wrapper freezeScore(@PathVariable String skuId, @PathVariable Integer num) {
        skuReference.freezeScore(skuId, num);
        return Wrappers.success();
    }

    @Override
    public Wrapper paySuccess(@PathVariable String spuId, @PathVariable String skuId, @PathVariable Integer num) {
        skuReference.paySuccess(spuId, skuId, num);
        return Wrappers.success();
    }

    @Override
    public Wrapper unfreezeScore(@PathVariable String skuId, @PathVariable Integer num) {
        skuReference.unfreezeScore(skuId, num);
        return Wrappers.success();
    }
}

