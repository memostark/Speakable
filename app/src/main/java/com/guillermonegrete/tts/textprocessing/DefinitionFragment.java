package com.guillermonegrete.tts.textprocessing;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.guillermonegrete.tts.databinding.FragmentProcessDefinitionBinding;
import com.guillermonegrete.tts.textprocessing.domain.model.WikiItem;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.List;

public class DefinitionFragment extends Fragment {

    private FragmentProcessDefinitionBinding binding;

    private static WiktionaryAdapter mAdapter;

    public static DefinitionFragment newInstance(WiktionaryAdapter adapter){
        DefinitionFragment fragment = new DefinitionFragment();

        // TODO Instead of sending the adapter, send a list of parcelable items
        mAdapter = adapter;

        return fragment;

    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentProcessDefinitionBinding.inflate(inflater, container, false);
        binding.recyclerViewWiki.setAdapter(mAdapter);
        return binding.getRoot();
    }

    public void updateData(List<WikiItem> items) {
        binding.recyclerViewWiki.setAdapter(new WiktionaryAdapter(items));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mAdapter = null;
        binding = null;
    }
}
