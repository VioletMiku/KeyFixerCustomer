package com.keyfixer.customer.Fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetDialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import com.keyfixer.customer.R;
import android.view.ViewGroup;

public class BottomSheetCustomerFragment extends BottomSheetDialogFragment {
    public String mTag;

    public static BottomSheetCustomerFragment newInstance (String mTag) {
        BottomSheetCustomerFragment f = new BottomSheetCustomerFragment();
        Bundle args = new Bundle();
        args.putString("TAG", mTag);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mTag = getArguments().getString("TAG");
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater , @Nullable ViewGroup container , @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottom_sheet_customer, container, false);
        return view;
    }
}
