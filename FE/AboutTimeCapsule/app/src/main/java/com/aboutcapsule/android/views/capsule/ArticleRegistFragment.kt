package com.aboutcapsule.android.views.capsule

import android.Manifest
import android.app.Activity.RESULT_OK
import android.app.DatePickerDialog
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.fragment.NavHostFragment
import com.aboutcapsule.android.R
import com.aboutcapsule.android.data.memory.MemoryRegistReq
import com.aboutcapsule.android.databinding.FragmentArticleRegistBinding
import com.aboutcapsule.android.factory.MemoryViewModelFactory
import com.aboutcapsule.android.model.MemoryViewModel
import com.aboutcapsule.android.repository.MemoryRepo
import com.aboutcapsule.android.util.GlobalAplication
import com.aboutcapsule.android.views.MainActivity
import com.bumptech.glide.Glide
import com.google.gson.Gson
import com.gun0912.tedpermission.PermissionListener
import com.gun0912.tedpermission.normal.TedPermission
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar


class ArticleRegistFragment : Fragment(),View.OnClickListener {
    /*
        상황에 따라 달력 보이게 안보이게 onCreate에서 받아온 데이터 체크 한다음 view visibility 정해주기
     */
    companion object {
        lateinit var imageList : ArrayList<MultipartBody.Part>
        lateinit var memoryViewModel: MemoryViewModel
        private lateinit var binding: FragmentArticleRegistBinding
        lateinit var navController: NavController
        private var picture_flag = 0
        private var flag = false
        private var currentUser = GlobalAplication.preferences.getInt("currentUser", -1)

        // 갤러리에서 데이터(사진) 가져올 때 사용
        lateinit var resultLauncher: ActivityResultLauncher<Intent>

        private var isGroup : Boolean = false // Board로 갈때 분기처리용
        private var isFirstGroup : Boolean = false // 최초 캡슐일 때
        private var title : String =""
        private var capsuleId : Int = 0
        private var lat : Double = 0.0
        private var lng : Double = 0.0
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater,R.layout.fragment_article_regist, container,false)

        // 상단 벨 숨기기
        bellToggle(true)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 바텀 네비 숨기기
        bottomNavToggle(true)

        navController = Navigation.findNavController(view)

        getDataFromBack()

    }

    fun getDataFromBack() {
        val memoryRepo = MemoryRepo()
        val memoryViewModelFactory = MemoryViewModelFactory(memoryRepo)
        memoryViewModel = ViewModelProvider  (this, memoryViewModelFactory).get(MemoryViewModel::class.java)

        getBundle()

        binding.galleryBtn.setOnClickListener(this)

        getGalleryData()

        getCalendarDate()

        bellToggle(true)

        setNavigation()

        setPage()

        redirectPage()
    }

    private fun setPage(){
        isGroup = requireArguments().getBoolean("isGroup",false)  // 그룹 여부
        isFirstGroup = requireArguments().getBoolean("isFirstGroup",false) // 그룹인데 봉인 하기 전인지 여부
        title=requireArguments().getString("capsuleTitle","null") // 제목 있을 경우

        if(isGroup){ // 그룹 일 경우
            if(isFirstGroup) { // 봉인 전 게시물일 경우
                binding.dateCommentlayout.visibility=View.GONE
                binding.articleRegistTitle.hint = "제목을 입력하세요"
            }else { // 봉인 후 게시물일 경우
                binding.dateCommentlayout.visibility=View.VISIBLE
                binding.articleRegistTitle.hint = "제목을 입력하세요"
            }
        }else { // 개인 일 경우
            binding.dateCommentlayout.visibility=View.VISIBLE
            binding.articleRegistTitle.setText(title)
        }
    }


    fun redirectPage() {
        lat = requireArguments().getDouble("lat", -1.0)
        lng = requireArguments().getDouble("lng", -1.0)

        // 추억 생성하기 버튼 클릭 시 ,
        binding.articleRegistRegistbtn.setOnClickListener {
            if (isGroup) { // 그룹
                if (isFirstGroup) { // 최초 그룹 일경우
                    val memberId = currentUser
                    val capsuleId = GlobalAplication.preferences.getInt("capsuleId", -1)
                    val title = binding.articleRegistTitle.text.toString()
                    val content = binding.articleContent.text.toString()

                    val memoryRegistReq =
                        MemoryRegistReq(memberId, capsuleId, title, content, null)
                    val memoryReqJson = Gson().toJson(memoryRegistReq)
                    val memoryReqBody =
                        memoryReqJson.toRequestBody("application/json".toMediaTypeOrNull())
                    Log.d("프래그먼트image", "${imageList}")
                    Log.d("프래그먼트req", "${memoryRegistReq}")
                    memoryViewModel.registerMemory(imageList, memoryReqBody)
                    val bundle = bundleOf()
                    bundle.putInt("capsuleId", capsuleId)
                    bundle.putDouble("lat", lat)
                    bundle.putDouble("lng", lng)
                    Log.d("추억 등록할 때, capsuleId", "${GlobalAplication.preferences.getInt("capsuleId", -1)}")
                    memoryViewModel.isRegister.observe(viewLifecycleOwner) {
                        navController.navigate(
                            R.id.action_articleRegistFragment_to_capsuleGroupFragment,
                            bundle
                        )
                    }

                } else { // 최초 그룹이 아닐 경우
                    val openDateString = binding.openAvailDate.text.toString()
                    if (openDateString.trim() == "지정하신날짜") {
                        Toast.makeText(requireContext(), "날짜를 지정해주세요", Toast.LENGTH_SHORT).show()
                    } else {
                        val memberId = currentUser
                        val capsuleId = GlobalAplication.preferences.getInt("capsuleId", -1)
                        val title = binding.articleRegistTitle.text.toString()
                        val content = binding.articleContent.text.toString()
                        val dataPattern = "yyyy년 M월 d일"
                        val formatter = DateTimeFormatter.ofPattern(dataPattern)
                        val localDate = LocalDate.parse(openDateString, formatter).toString()
                        Log.d("localDate", "${localDate}")

                        val memoryRegistReq =
                            MemoryRegistReq(memberId, capsuleId, title, content, localDate)
                        val memoryReqJson = Gson().toJson(memoryRegistReq)
                        val memoryReqBody =
                            memoryReqJson.toRequestBody("application/json".toMediaTypeOrNull())
                        Log.d("프래그먼트image", "${imageList}")
                        Log.d("프래그먼트req", "${memoryRegistReq}")
                        memoryViewModel.registerMemory(imageList, memoryReqBody)
                        val bundle = bundleOf()
                        bundle.putInt("capsuleId", capsuleId)
                        bundle.putDouble("lat", lat)
                        bundle.putDouble("lng", lng)
                        memoryViewModel.isRegister.observe(viewLifecycleOwner) {
                            navController.navigate(
                                R.id.action_articleRegistFragment_to_capsuleGroupFragment,
                                bundle
                            )
                        }



                    }
                }
            } else {  // 개인

                val openDateString = binding.openAvailDate.text.toString()
                if (openDateString.trim() == "지정하신날짜") {
                    Toast.makeText(requireContext(), "날짜를 지정해주세요", Toast.LENGTH_SHORT).show()
                } else {
                    val memberId = currentUser
                    val capsuleId = GlobalAplication.preferences.getInt("capsuleId", -1)
                    val title = binding.articleRegistTitle.text.toString()
                    val content = binding.articleContent.text.toString()
                    val dataPattern = "yyyy년 M월 d일"
                    val formatter = DateTimeFormatter.ofPattern(dataPattern)
                    val localDate = LocalDate.parse(openDateString, formatter).toString()
                    Log.d("localDate", "${localDate}")

                    val memoryRegistReq =
                        MemoryRegistReq(memberId, capsuleId, title, content, localDate)
                    Log.d("메모리레지스트원본" , "${MemoryRegistReq(memberId, capsuleId, title, content, localDate)}")
                    val memoryReqJson = Gson().toJson(memoryRegistReq)
                    val memoryReqBody =
                        memoryReqJson.toRequestBody("application/json".toMediaTypeOrNull())
                    Log.d("프래그먼트image", "${imageList}")
                    Log.d("프래그먼트req", "${memoryRegistReq}")
                    memoryViewModel.registerMemory(imageList, memoryReqBody)

                    navController.navigate(
                        R.id.action_articleRegistFragment_to_mainPageMainFragment

                    )
                }
            }
        }
    }

    // 네비게이션 세팅
    private fun setNavigation(){
        val navHostFragment =requireActivity().supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        CapsuleRegistFragment.navController = navHostFragment.navController
    }

    fun getCalendarDate(){

        binding.dateCommentlayout.setOnClickListener {
            flag = true

            if(!flag){
                binding.dateCommentlayout.visibility=View.VISIBLE
                binding.datepickedlayout.visibility=View.GONE
            }else{
                binding.dateCommentlayout.visibility = View.GONE
                binding.datepickedlayout.visibility=View.VISIBLE
            }

            val cal = Calendar.getInstance()

            val data = DatePickerDialog.OnDateSetListener { _, year, month, day ->
                binding.openAvailDate.text = "${year}년 ${month+1}월 ${day}일"
            }

            val textColor= ContextCompat.getColor(requireContext(),R.color.datePickerColor)

            val datePickerDialog = DatePickerDialog(requireContext(),R.style.MyDatePicker ,data, cal.get(Calendar.YEAR),cal.get(Calendar.MONTH),cal.get(Calendar.DAY_OF_MONTH))

            datePickerDialog.show()

            val posBtn =datePickerDialog.getButton(DatePickerDialog.BUTTON_POSITIVE)
            val negBtn =datePickerDialog.getButton(DatePickerDialog.BUTTON_NEGATIVE)
            posBtn.setText("확인")
            negBtn.setText("취소")
            posBtn.setTextColor(textColor)
            negBtn.setTextColor(textColor)
        }

        binding.datepickedlayout.setOnClickListener {
            flag = true
            val cal = Calendar.getInstance()

            val data = DatePickerDialog.OnDateSetListener { _, year, month, day ->
                binding.openAvailDate.text = "${year}년 ${month+1}월 ${day}일"
            }

            val textColor= ContextCompat.getColor(requireContext(),R.color.datePickerColor)

            val datePickerDialog = DatePickerDialog(requireContext(),R.style.MyDatePicker ,data, cal.get(Calendar.YEAR),cal.get(Calendar.MONTH),cal.get(Calendar.DAY_OF_MONTH))

            datePickerDialog.show()

            val posBtn =datePickerDialog.getButton(DatePickerDialog.BUTTON_POSITIVE)
            val negBtn =datePickerDialog.getButton(DatePickerDialog.BUTTON_NEGATIVE)
            posBtn.setText("확인")
            negBtn.setText("취소")
            posBtn.setTextColor(textColor)
            negBtn.setTextColor(textColor)
        }
    }

    // 상단바 벨 사라지게 / 페이지 전환 시 다시 생성
    private fun bellToggle(sign : Boolean){
        var bell = activity?.findViewById<ImageView>(R.id.toolbar_bell)
        if(sign) {
            bell?.visibility = View.GONE
        }else{
            bell?.visibility = View.VISIBLE
        }
    }

    fun getGalleryData(){
        resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult())
        {
            if(it.resultCode == RESULT_OK){
                Log.d("갤러리getdata()", "${it.data}")
                Log.d("갤러리getdata()", "${it.data?.data}")
                if(picture_flag == 1){
                    it.data?.data?.let { uri ->
                        val imageUri: Uri? = it.data?.data

                        if(imageUri != null){
                            imageList = ArrayList<MultipartBody.Part>()
                            val filePath: String = getRealPathFromUri(imageUri!!)
                            val imageFile = File(filePath)
                            val requestBody: RequestBody = imageFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
                            val multipartBodyPart: MultipartBody.Part = MultipartBody.Part.createFormData("image",
                                 "${currentUser}${imageUri}.jpg", requestBody)
//                            val multipartBody = convertUrlToMultipart(imagefile!!)
                            imageList.add(multipartBodyPart)
                            activity?.applicationContext?.let { it1 ->
                                Glide.with(it1).load(imageUri).override(500,500)
                                    .into(binding.selectedPhoto)
                                Log.d("photo", "${binding.selectedPhoto}")
                            }
                        }
                    }
                }
            }
        }
    }
    fun getRealPathFromUri(uri: Uri) : String {
        var realPath = ""
        val proj = arrayOf(MediaStore.Images.Media.DATA)
        val cursor: Cursor? = requireActivity().contentResolver.query(uri, proj, null, null, null)
        if (cursor != null) {
            val columnIndex: Int = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            cursor.moveToFirst()
            realPath = cursor.getString(columnIndex)
            cursor.close()
        }
        return realPath
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

    // 바텀 네비 숨기기 토글
    private fun bottomNavToggle(sign : Boolean){
        val mainActivity = activity as MainActivity
        mainActivity.hideBottomNavi(sign)
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

    // 번들 값 가져오기
    private fun getBundle() {
//        title = requireArguments().getString("capsuleTitle")!!
//        binding.articleRegistTitle.text = title
//        whoAreYouFlag = requireArguments().getString("whoAreYouFlag","noValue")
        capsuleId = GlobalAplication.preferences.getInt("registCapsuleId",-1)
    }

    override fun onDestroy() {
        // 상단 벨 다시 살리기
        bellToggle(false)

        // 바텀 네비 다시 살리기
        bottomNavToggle(false)

        super.onDestroy()
    }

}