package com.comino.flight.ui.widgets.charts.utils;


import java.util.List;

import com.sun.javafx.collections.ObservableListWrapper;

/**
 * A List wrapper class that implements observability.
 *
 */
public class XYObservableListWrapper<E> extends ObservableListWrapper<E>  {
	
	private int startCount=0;

	public XYObservableListWrapper(List<E> list) {
		super(list);
	}
	
	public void begin() {
		startCount = modCount;
		beginChange();
	}
	
	public int end() {
		endChange();
		return modCount-startCount;
	}
	
	@Override
    public void add(int index, E element) {
        doAdd(index, element);
        nextAdd(index, index + 1);
        ++modCount; 
    }
	
	@Override
    public E remove(int index) {
        E old = doRemove(index);
        nextRemove(index, old);
        ++modCount;
        return old;
    }

    

}
