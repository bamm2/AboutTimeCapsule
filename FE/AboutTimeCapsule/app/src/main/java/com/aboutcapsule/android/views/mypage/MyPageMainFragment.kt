package com.aboutcapsule.android.views.mypage

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.aboutcapsule.android.R
import com.aboutcapsule.android.databinding.FragmentMyPageMainBinding
import com.aboutcapsule.android.factory.MyPageViewModelFactory
import com.aboutcapsule.android.model.MyPageViewModel
import com.aboutcapsule.android.repository.mypage.MypageRepo
import com.aboutcapsule.android.util.RetrofitManager


class MyPageMainFragment : Fragment() {

    private lateinit var binding: FragmentMyPageMainBinding
    private lateinit var navController: NavController

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        getMyPageFriendRequestList()
        getMyPageDataFromBack()

        // 네비게이션 세팅
        setNavigation()
        // 페이지 이동
        redirectPage()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_my_page_main, container, false)
        return binding.root

    }

    // 유저 프로필정보 가져오기
    fun getMyPageProfileInfo() {

    }


    // 친구목록 썸네일들 띄워주기
    fun getMyPageFriendList() {

    }


    // 네비게이션 세팅
    private fun setNavigation(){
        val navHostFragment =requireActivity().supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController
    }
    private fun redirectPage(){
        // 상단 툴바 알림페이지로 리다이렉트
        val notiBtn = activity?.findViewById<ImageView>(R.id.toolbar_bell)
        notiBtn?.setOnClickListener{
            navController.navigate(R.id.action_myPageMainFragment_to_notificationMainFragment)
        }
    }

    // 친구요청목록리스트띄워주기
    fun getMyPageFriendRequestList() {
        val friendRequestData = getFriendRequestData()
        val friendRequestAdapter = MyPageFriendRequestAdapter()
        friendRequestAdapter.itemList = friendRequestData
        binding.myPageFriendRequestView.adapter = friendRequestAdapter
        binding.myPageFriendRequestView.layoutManager =
            LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
    }

    // retrofit으로 api요청을 통해 데이터 가져와야됨.
    fun getFriendRequestData() :MutableList<MyPageFriendRequestData>{

        var itemList = mutableListOf<MyPageFriendRequestData>()

        for (i in 1..5) {
            var username = "엄준식${i}"
            var img = R.drawable.kakao_login_symbol
            val tmp = MyPageFriendRequestData(img, username)
            itemList.add(tmp)
        }
        return itemList
    }
    fun getMyPageDataFromBack() {
        val repository = MypageRepo()
        val myPageViewModelFactory = MyPageViewModelFactory(repository)
        var viewModel: MyPageViewModel = ViewModelProvider  (this, myPageViewModelFactory).get(MyPageViewModel::class.java)
        viewModel.getMyPage(1)
        viewModel.myPageList.observe(viewLifecycleOwner, Observer {
            Log.e("응답옴", "응애3")
        })


    }








}