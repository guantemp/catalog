/*
 * Copyright (c) 2019. www.hoprxi.com All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package catalog.hoprxi.core.domain.model.price;

import catalog.hoprxi.core.domain.Validator;

import javax.money.MonetaryAmount;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.1 2019-09-03
 */
public class Price {
    private String skuId;
    private String roleId;
    private MonetaryAmount amount;

    public Price(String skuId, String roleId, MonetaryAmount amount) {
        setSkuId(skuId);
        setRoleId(roleId);
        setAmount(amount);
    }

    private void setSkuId(String skuId) {
        this.skuId = skuId;
    }

    private void setRoleId(String roleId) {
        if (!Validator.isRoleExist(roleId))
            throw new IllegalArgumentException("role isn't exist");
        this.roleId = roleId;
    }

    private void setAmount(MonetaryAmount amount) {
        this.amount = amount;
    }

    public String skuId() {
        return skuId;
    }

    public String roleId() {
        return roleId;
    }

    public MonetaryAmount amount() {
        return amount;
    }

    public void changAmount(MonetaryAmount amount) {
        if (!this.amount.isEqualTo(amount))
            this.amount = amount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Price price = (Price) o;

        if (skuId != null ? !skuId.equals(price.skuId) : price.skuId != null) return false;
        return roleId != null ? roleId.equals(price.roleId) : price.roleId == null;
    }

    @Override
    public int hashCode() {
        int result = skuId != null ? skuId.hashCode() : 0;
        result = 31 * result + (roleId != null ? roleId.hashCode() : 0);
        return result;
    }
}
