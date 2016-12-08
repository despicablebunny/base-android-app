package com.ww.lp.base.modules.order.post;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.ww.lp.base.BR;
import com.ww.lp.base.BaseActivity;
import com.ww.lp.base.BaseFragment;
import com.ww.lp.base.R;
import com.ww.lp.base.components.rvrl.LPRecyclerViewAdapter;
import com.ww.lp.base.components.rvrl.SingleItemClickListener;
import com.ww.lp.base.data.CarouselInfo;
import com.ww.lp.base.data.ProjectDetail;
import com.ww.lp.base.data.ProjectInfo;
import com.ww.lp.base.databinding.PostFragBinding;
import com.ww.lp.base.entity.ImageInfo;
import com.ww.lp.base.entity.ProjectPostInfo;
import com.ww.lp.base.utils.ToastUtils;

import java.util.ArrayList;
import java.util.List;

import me.iwf.photopicker.PhotoPicker;

import static android.app.Activity.RESULT_OK;
import static com.google.gson.internal.$Gson$Preconditions.checkNotNull;

/**
 * Created by LinkedME06 on 16/11/13.
 */

public class PostFragment extends BaseFragment implements PostContract.View {

    private PostFragBinding binding;
    private PostContract.Presenter mPresenter;
    private LinearLayoutManager mLayoutManager;
    private ArrayList<ImageInfo> mRVData = new ArrayList<>();
    private LPRecyclerViewAdapter<ImageInfo> lpRecyclerViewAdapter;
    private ArrayList<CarouselInfo> imgCarouseList = new ArrayList<>();
    private boolean isAdd = true;

    public static PostFragment newInstance(Intent intent) {

        Bundle args = new Bundle();
        if (intent != null) {
            args.putParcelable("projectDetail", intent.getParcelableExtra("projectDetail"));
        }
        PostFragment fragment = new PostFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onResume() {
        super.onResume();
        mPresenter.subscribe();
    }

    @Override
    public void onPause() {
        super.onPause();
        mPresenter.unsubscribe();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = onCreateView(inflater, container, savedInstanceState, R.layout.post_frag, false);
        binding = PostFragBinding.bind(root);
        ProjectPostInfo projectPostInfo = new ProjectPostInfo();
        if (getArguments().getParcelable("projectDetail") != null) {
            ProjectInfo projectInfo = ((ProjectDetail) getArguments().getParcelable("projectDetail")).getProjectInfo();
            projectPostInfo.setTitle(projectInfo.getTitle());
            projectPostInfo.setDescribe(projectInfo.getDescribe());
            projectPostInfo.setPrice(projectInfo.getPrice());
            projectPostInfo.setPhoneNum(projectInfo.getPhoneNum());
            projectPostInfo.setProjectId(projectInfo.getProjectId());
            projectPostInfo.setImg(projectInfo.getImg());
            imgCarouseList = ((ProjectDetail) getArguments().getParcelable("projectDetail")).getCarouselInfo();
            projectPostInfo.setImgs(imgCarouseList);
            isAdd = false;
            binding.btnPost.setText("修改需求");
        }
        binding.setProjectPostInfo(projectPostInfo);
        binding.lpRv.setHasFixedSize(true);
        // use a linear layout manager
        mLayoutManager = new LinearLayoutManager(getActivity());
        mLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        binding.lpRv.setLayoutManager(mLayoutManager);
        lpRecyclerViewAdapter = new LPRecyclerViewAdapter<>(mRVData, R.layout.post_img_item, BR.lp_rv_item);
        binding.lpRv.setAdapter(lpRecyclerViewAdapter);
        binding.lpRv.addOnItemTouchListener(new SingleItemClickListener(binding.lpRv, new SingleItemClickListener.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
            }

            @Override
            public void onItemLongClick(View view, int position) {
                deleteImg(position);
                ToastUtils.toastLong("删除图片成功！");
            }
        }));
        binding.imgPicker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PhotoPicker.builder()
                        .setPhotoCount(4)
                        .setShowCamera(true)
                        .setPreviewEnabled(true)
                        .start(getActivity(), PostFragment.this, PhotoPicker.REQUEST_CODE);
            }
        });
        binding.btnPost.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ProjectPostInfo projectPostInfo = binding.getProjectPostInfo();
                projectPostInfo.setImgs(imgCarouseList);
                mPresenter.post(projectPostInfo, isAdd);
            }
        });
        if (!isAdd){
            showImgList();
        }
        return root;
    }

    @Override
    public void setPresenter(@NonNull PostContract.Presenter presenter) {
        mPresenter = checkNotNull(presenter);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK &&
                (requestCode == PhotoPicker.REQUEST_CODE)) {

            List<String> photos = null;
            if (data != null) {
                photos = data.getStringArrayListExtra(PhotoPicker.KEY_SELECTED_PHOTOS);
                ((BaseActivity) getActivity()).showProgressDialogLP("正在上传图片，请稍后...");
                mPresenter.uploadFile((ArrayList<String>) photos);
            }
        }
    }

    @Override
    public void uploadFileResult(boolean result, ArrayList<String> imgList) {
        ((BaseActivity) getActivity()).removeProgressDialogLP();
        if (result) {
            if (isAdd) {
                mRVData.clear();
                imgCarouseList.clear();
            }
            for (int i = 0; i < imgList.size(); i++) {
                ImageInfo imageInfo = new ImageInfo();
                imageInfo.setUri(Uri.parse(imgList.get(i)));
                mRVData.add(imageInfo);
                CarouselInfo carouselInfo = new CarouselInfo();
                carouselInfo.setImg(imgList.get(i));
                imgCarouseList.add(carouselInfo);
            }
            lpRecyclerViewAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void addOrModifySuccess(String status) {
        if (status.equals("200")){
            if (isAdd){
                ToastUtils.toastShort("发布成功");
            }else{
                ToastUtils.toastShort("修改成功");
            }
            getActivity().finish();
        }else{
            if (isAdd){
                ToastUtils.toastShort("发布失败，请重试~");
            }else{
                ToastUtils.toastShort("修改失败，请重试~");
            }
        }
    }


    public void showImgList() {
        for (int i = 0; i < imgCarouseList.size(); i++) {
            ImageInfo imageInfo = new ImageInfo();
            imageInfo.setUri(Uri.parse(imgCarouseList.get(i).getImg()));
            mRVData.add(imageInfo);
        }
        lpRecyclerViewAdapter.notifyDataSetChanged();
    }

    public void deleteImg(int position) {
        mRVData.remove(position);
        imgCarouseList.remove(position);
        lpRecyclerViewAdapter.notifyDataSetChanged();
    }

}
