/*
 *  Copyright (c) 2019. www.foxtail.cc All Rights Reserved.
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

package catalog.hoprxi.core.domain.model;

import java.time.LocalDate;
import java.util.Set;
import java.util.TreeSet;

/***
 * @author <a href="www.foxtail.cc/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.1 2019-04-29
 */
public class ShelfLifeService {
    private Set<OnSchedule> onScheduleSet = new TreeSet<>();


    public boolean isOnSchedule(Sku sku, LocalDate dateOfManufacture) {
        return false;
    }

    public boolean isOverdueCommodity(Sku sku, LocalDate dateOfManufacture) {
        return false;
    }

    public static class OnSchedule implements Comparable<OnSchedule> {
        private int thresholdDays;
        private int onScheduleDays;

        public OnSchedule(int thresholdDays, int onScheduleDays) {
            setThresholdDays(thresholdDays);
            setOnScheduleDays(onScheduleDays);
        }

        private void setThresholdDays(int thresholdDays) {
            if (thresholdDays <= 0)
                throw new IllegalArgumentException("");
            this.thresholdDays = thresholdDays;
        }

        private void setOnScheduleDays(int onScheduleDays) {
            if (onScheduleDays <= 0)
                throw new IllegalArgumentException("");
            this.onScheduleDays = onScheduleDays;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            OnSchedule that = (OnSchedule) o;

            if (thresholdDays != that.thresholdDays) return false;
            return onScheduleDays == that.onScheduleDays;
        }

        @Override
        public int hashCode() {
            int result = thresholdDays;
            result = 31 * result + onScheduleDays;
            return result;
        }

        public int thresholdDays() {
            return thresholdDays;
        }

        public int onScheduleDays() {
            return onScheduleDays;
        }

        @Override
        public int compareTo(OnSchedule o) {
            return 0;
        }
    }
}
