package com.geeenraisekeyer.fragment;

import android.content.Intent;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.geeenraisekeyer.R;
import com.geeenraisekeyer.activity.CameraActivity;
import com.geeenraisekeyer.activity.LoginActivity;
import com.geeenraisekeyer.utils.AppData;
import com.geeenraisekeyer.utils.PhoneUtil;

import butterknife.BindView;
import butterknife.OnClick;

/**
 * Created by ASUS on 2017/5/17.
 */

public class LoginFragment extends BaseFragment {

    @BindView(R.id.et_account)
    EditText et_account;
    @BindView(R.id.et_password)
    EditText et_password;
    @BindView(R.id.tv_login)
    TextView tv_login;
    @BindView(R.id.tv_version)
    TextView tv_version;


    @Override
    protected int getContentLayout() {
        return R.layout.fragment_login;
    }

    @OnClick({R.id.tv_login})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.tv_login:
                toLogin();
                break;
        }
    }

    private void toLogin() {
        startActivity(new Intent(mContext, CameraActivity.class));
        ((LoginActivity)getActivity()).finish();
//        final String account = et_account.getText().toString().trim();
//        final String passwd = et_password.getText().toString().trim();
//        if(TextUtils.isEmpty(account)){
//            ToastUtils.showCustomerToast(mContext,"请输入账号");
//            return;
//        }
//        if(TextUtils.isEmpty(passwd)){
//            ToastUtils.showCustomerToast(mContext,"请输入密码");
//            return;
//        }
//        if(!NetworkUtils.isNetworkAvailable(mContext)){
//            ToastUtils.showCustomerToast(mContext,"请检查网络连接");
//            return;
//        }
//        tv_login.setEnabled(false);
//        StringRequest request = new StringRequest(Request.Method.POST, RequestUtil.login, new Listener<String>() {
//            @Override
//            public void onSuccess(String response) {
//                try {
//                    tv_login.setEnabled(true);
//                    JSONObject obj = new JSONObject(response);
//                    if (obj.getInt("errCode") ==1) {
//                        AppData.saveString(mContext, AppData.ACCOUNT, account.toUpperCase());
//                        AppData.saveString(mContext, AppData.PASSWORD, passwd);
//                        startActivity(new Intent(mContext, CameraActivity.class));
//                        ((LoginActivity)getActivity()).finish();
//                    }else {
//                        ToastUtils.showSystemToast(mContext,obj.getString("desc"));
//                    }
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//            }
//
//            @Override
//            public void onError(VolleyError error) {
//                Log.d("error","");
//                tv_login.setEnabled(true);
//            }
//        }){
//            @Override
//            protected Map<String, String> getParams() throws AuthFailureError {
//                Map<String, String> map = new HashMap<>();
//                map.put("account", account.toUpperCase());
//                map.put("password", passwd);
//                map.put("device_id", PhoneUtil.getDevicesID());
//                map.put("model", PhoneUtil.getBrand());
//                map.put("version_name",PhoneUtil.getVersionName(mContext));
//                map.put("version_code",PhoneUtil.getVersionCode(mContext));
//                map.put("os_version", PhoneUtil.getSystemVersion());
//                return map;
//            }
//        };
//        Volley.getRequestQueue().add(request);
    }

    @Override
    protected void preInitData() {
        et_account.setText(AppData.getString(mContext, AppData.ACCOUNT));
        et_password.setText(AppData.getString(mContext, AppData.PASSWORD));
        tv_version.setText("当前版本:"+PhoneUtil.getVersionName(mContext));
    }
}
