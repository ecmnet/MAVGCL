package com.comino.flight.ui.widgets.charts.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.RandomAccess;

import com.sun.javafx.collections.ObservableListWrapper;
import com.sun.javafx.collections.ObservableSequentialListWrapper;

import javafx.collections.ObservableList;

public class XYCollections {
	
	@SuppressWarnings("unchecked")
	public static <E> ObservableList<E> observableArrayList() {
        return observableList(new ArrayList());
    }
	
	public static <E> ObservableList<E> observableList(List<E> list) {
        if (list == null) {
            throw new NullPointerException();
        }
        return list instanceof RandomAccess ? new XYObservableListWrapper<E>(list) :
                new ObservableSequentialListWrapper<E>(list);
    }

}
