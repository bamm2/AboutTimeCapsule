package com.aboutcapsule.android.views.capsule

import android.Manifest
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.registerForActivityResult
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.aboutcapsule.android.R
import com.aboutcapsule.android.databinding.FragmentArticleRegistBinding
import com.bumptech.glide.Glide
import com.gun0912.tedpermission.PermissionListener
import com.gun0912.tedpermission.normal.TedPermission

class ArticleRegistFragment : Fragment(),View.OnClickListener {

    private lateinit var binding : FragmentArticleRegistBinding
    private var picture_flag = 0
    private var fileAbsolutePath: String? = null

    // 갤러리에서 데이터(사진) 가져올 때 사용
    lateinit var resultLauncher: ActivityResultLauncher<Intent>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater,R.layout.fragment_article_regist,container,false)

        binding.galleryBtn.setOnClickListener(this)

        getData()

        return binding.root
    }

    fun getData(){
        resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult())
        {
            if(it.resultCode == RESULT_OK){
                if(picture_flag == 1){
                    it.data?.data?.let { uri ->
                        val imageUri: Uri? = it.data?.data
                        if(imageUri != null){
                            activity?.applicationContext?.let { it1 ->
                                Glide.with(it1).load(imageUri).override(500,500)
                                    .into(binding.selectedPhoto)
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onClick(v: View?) {
        when(v?.id){
            binding.galleryBtn.id -> { settingPermission(1)}

        }
    }

    fun settingPermission(permis_num: Int){
        val permis = object : PermissionListener {

            override fun onPermissionGranted(){
                if(permis_num == 1) {
                    move_gallery()
                }
            }

            override fun onPermissionDenied(deniedPermission: MutableList<String>?){}
        }
        if(permis_num == 1){
            checkPer_gallery(permis)
        }
    }

    // 갤러리로 이동
    fun move_gallery(){
        val intent = Intent(Intent.ACTION_PICK)
        intent.data = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        intent.type = "image/*"
        // 이미지 여러장 선택하기
        // 페이지 다시 이동
        resultLauncher.launch(intent)
        // 갤러리로
        picture_flag = 1
    }

    // 갤러리 관련 권한 체크
    fun checkPer_gallery(permis: PermissionListener){
        TedPermission.create()
            .setPermissionListener(permis)
            .setDeniedMessage("[설정] > [권한] 에서 권한을 허용할 수 있습니다.")
            .setPermissions(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
            ).check()
    }

}