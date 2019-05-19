package com.keyfixer.customer.Fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetDialogFragment;
import android.view.LayoutInflater;
import android.view.View;

import com.keyfixer.customer.Common.Common;
import com.keyfixer.customer.R;
import android.view.ViewGroup;
import android.widget.ImageView;

public class BottomSheetCustomerFragment extends BottomSheetDialogFragment {
    public String mTag;
    private ModalBottomSheet modalBottomSheet;

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

        final ImageView img_fix_home_service = view.findViewById(R.id.fix_home_service);
        final ImageView img_fix_car_service = view.findViewById(R.id.fix_car_service);
        final ImageView img_fix_motorbike_service = view.findViewById(R.id.fix_motorbike_service);

        if (Common.service_want_to_fix == "S ử a   k h ó a   n h à"){
            img_fix_home_service.setImageResource(R.drawable.icons8_real_estate_96_chose);
            img_fix_car_service.setImageResource(R.drawable.icons8_car_rental_96);
            img_fix_motorbike_service.setImageResource(R.drawable.icons8_motorcycle_96);
        }
        if (Common.service_want_to_fix == "S ử a   k h ó a   x e   h ơ i"){
            img_fix_home_service.setImageResource(R.drawable.icons8_real_estate_96);
            img_fix_car_service.setImageResource(R.drawable.icons8_car_rental_96_chose);
            img_fix_motorbike_service.setImageResource(R.drawable.icons8_motorcycle_96);
        }
        if (Common.service_want_to_fix == "S ử a   k h ó a   x e   g ắ n   m á y"){
            img_fix_home_service.setImageResource(R.drawable.icons8_real_estate_96);
            img_fix_car_service.setImageResource(R.drawable.icons8_car_rental_96);
            img_fix_motorbike_service.setImageResource(R.drawable.icons8_motorcycle_96_chose);
        }

        img_fix_home_service.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                modalBottomSheet.onImageViewCLicked(true, false, false);
                img_fix_home_service.setImageResource(R.drawable.icons8_real_estate_96_chose);
                img_fix_car_service.setImageResource(R.drawable.icons8_car_rental_96);
                img_fix_motorbike_service.setImageResource(R.drawable.icons8_motorcycle_96);
                Common.service_want_to_fix = "S ử a   k h ó a   n h à";
            }
        });
        img_fix_car_service.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                modalBottomSheet.onImageViewCLicked(false, true, false);
                img_fix_home_service.setImageResource(R.drawable.icons8_real_estate_96);
                img_fix_car_service.setImageResource(R.drawable.icons8_car_rental_96_chose);
                img_fix_motorbike_service.setImageResource(R.drawable.icons8_motorcycle_96);
                Common.service_want_to_fix = "S ử a   k h ó a   x e   h ơ i";
            }
        });
        img_fix_motorbike_service.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                modalBottomSheet.onImageViewCLicked(false, false, true);
                img_fix_home_service.setImageResource(R.drawable.icons8_real_estate_96);
                img_fix_car_service.setImageResource(R.drawable.icons8_car_rental_96);
                img_fix_motorbike_service.setImageResource(R.drawable.icons8_motorcycle_96_chose);
                Common.service_want_to_fix = "S ử a   k h ó a   x e   g ắ n   m á y";
            }
        });
        return view;
    }

    public interface ModalBottomSheet {
        void onImageViewCLicked(boolean isFix_home_service, boolean isFix_car_service, boolean isFix_motorbike_service);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            modalBottomSheet = (ModalBottomSheet) context;
        } catch(ClassCastException e){
            throw new ClassCastException(context.toString() + " must implement ModalBottomSheet");
        }
    }
}
