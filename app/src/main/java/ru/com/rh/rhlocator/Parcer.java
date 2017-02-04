package ru.com.rh.rhlocator;

import android.location.Location;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.ResultReceiver;

/**
 * Этот класс создан потому, что на некоторых устройствах (В частности на эмуляторе BlueStacks
 * приложение вывливалось с ошибкой "Unmarshalling unknown type code", если передавать
 * через интент более одного объекта Parcelable.
 * Пришлось создать класс-контейнер для обоих объектов.
 * Возьмите меня на работу, я умный =)
 */

class Parcer implements Parcelable {
    ResultReceiver resultReceiver;
    Location location;

    Parcer(ResultReceiver resultReceiver, Location location) {
        this.resultReceiver = resultReceiver;
        this.location = location;
    }

    private Parcer(Parcel in) {
        resultReceiver = ResultReceiver.CREATOR.createFromParcel(in);
        location = Location.CREATOR.createFromParcel(in);
    }

    public static final Creator<Parcer> CREATOR = new Creator<Parcer>() {
        @Override
        public Parcer createFromParcel(Parcel in) {
            return new Parcer(in);
        }

        @Override
        public Parcer[] newArray(int size) {
            return new Parcer[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        resultReceiver.writeToParcel(dest, 0);
        location.writeToParcel(dest, 0);
    }

}
