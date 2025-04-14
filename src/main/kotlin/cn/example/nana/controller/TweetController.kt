package cn.example.nana.controller

import cn.example.nana.models.cassandra.Tweet
import cn.example.nana.repo.cassandra.TweetRepository
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * @Author  RichardYoung
 * @Description
 * @Date  2025/4/15 00:33
 */
@RestController
@RequestMapping("tweet")
class TweetController(
    private val tweetRepository: TweetRepository
){

    @RequestMapping("tweetList")
    fun tweet(@RequestParam(value = "page", defaultValue = "0") page: Int,@RequestParam(value = "pageSize", defaultValue = "10") pageSize: Int): MutableList<Tweet> {
        val pageRequest = PageRequest.of(page, pageSize, Sort.by(Sort.Direction.DESC, "timestamp"))
        val tweetPage = tweetRepository.findAll(pageRequest)
        return tweetPage.content
    }

}