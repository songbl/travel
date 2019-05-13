package cn.itcast.travel.service.impl;

import cn.itcast.travel.dao.CategoryDao;
import cn.itcast.travel.dao.impl.CategoryDaoImpl;
import cn.itcast.travel.domain.Category;
import cn.itcast.travel.service.CategoryService;
import cn.itcast.travel.util.JedisUtil;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Tuple;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 对于缓存的分析
 *      分析发现，分类的数据在每一次页面加载后都要重新请求数据库，通过查询，返回数据，
 *  对数据库的压力比较大，而且分类的数据不会经常发生改变，所以可以使用Redis来缓存这个数据
 */
public class CategoryServiceImpl implements CategoryService {

    private CategoryDao categoryDao = new CategoryDaoImpl();

    @Override
    public List<Category> findAll() {
        //==处理分类的情况，不是经常变化，且经常访问，放在Redis中

        //1.从redis中查询
        //1.1获取jedis客户端
        Jedis jedis = JedisUtil.getJedis();
        //1.2可使用sortedset排序查询（期望数据中存储的顺序就是将来展示的顺序；以i增加的方式存储的）
        // Set<String> categorys1 = jedis.zrange("category", 0, -1);//只查出来了值，没有取出分数
        //1.3查询sortedset中的分数(cid)和值(cname)
        Set<Tuple> categorys = jedis.zrangeWithScores("category", 0, -1);


        List<Category> cs = null;
        //2.判断查询的集合是否为空
        if (categorys == null || categorys.size() == 0) {

            System.out.println("从数据库查询....");
            //3.如果为空,需要从数据库查询,在将数据存入redis
            //3.1 从数据库查询
            cs = categoryDao.findAll();
            //3.2 将集合数据存储到redis中的 category的key
            for (int i = 0; i < cs.size(); i++) {
                //为了取出也是顺序的，所以存储的时候，按照id为分数进行存储的
                jedis.zadd("category", cs.get(i).getCid(), cs.get(i).getCname());
            }
        } else {
            System.out.println("从redis中查询.....");


            cs = new ArrayList<Category>();
            //4.如果不为空,将set的数据存入list
//            for (String name:categorys1){
//                Category category = new Category();
//                category.setCname(name);;
//                cs.add(category);
//            }
            for (Tuple tuple : categorys) {
                Category category = new Category();
                category.setCname(tuple.getElement());
                category.setCid((int)tuple.getScore());
                cs.add(category);

            }
        }


        return cs;
    }
}
