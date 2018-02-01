package com.bolyndevelopment.owner.runlogger2;

import android.support.annotation.Nullable;
import android.support.v7.util.DiffUtil;

import java.util.List;

//Created by Bobby Jones on 1/31/2018.

public class ListDiffCallback extends DiffUtil.Callback {
    List<ListItem> oldList;
    List<ListItem> newList;

    public ListDiffCallback (List<ListItem> oldList, List<ListItem> newList) {
        this.oldList = oldList;
        this.newList = newList;
    }

    @Override
    public int getOldListSize() {
        return oldList.size();
    }

    @Override
    public int getNewListSize() {
        return newList.size();
    }

    @Override
    public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
        return oldList.get(oldItemPosition).getId() == newList.get(newItemPosition).getId();
    }

    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
        return oldList.get(oldItemPosition).equals(newList.get(newItemPosition));
    }

    @Nullable
    @Override
    public Object getChangePayload(int oldItemPosition, int newItemPosition) {
        return super.getChangePayload(oldItemPosition, newItemPosition);
    }
}
