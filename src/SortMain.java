import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class SortMain {

    public static void main(String[] args) throws Exception{
        Integer threadNumber = 4; //线程数量
        Integer stopNumber = threadNumber;   //闭合器数量
        Integer randomCount = 50; //随机数数量
        Integer randomNumber = 50; //随机数范围
        List<Integer> workSpeace = new ArrayList<>(); //每个线程需要工作的量 list的长度与线程量一样
        if(randomCount % threadNumber == 0){
            for(int i=0; i < threadNumber; i ++){
                workSpeace.add(randomCount/threadNumber);
            }
        }else{
            Integer itemValue = randomCount/threadNumber;
            for(int i = 0 ; i < threadNumber ; i ++){
                if(i == threadNumber -1){
                    workSpeace.add((randomCount-(itemValue*threadNumber)) + itemValue);
                }else{
                    workSpeace.add(itemValue);
                }
            }
        }
        CountDownLatch countDownLatch = new CountDownLatch(stopNumber); //闭合器子线程闭合器

        CountDownLatch countDownLatchMain = new CountDownLatch(stopNumber); //主线程闭合器

        ExecutorService executors = Executors.newFixedThreadPool(threadNumber); //线程池
        Lock lock = new ReentrantLock();
        List<Integer> arrayList = new ArrayList<>();
        Random random = new Random(System.currentTimeMillis());
        try {
            for(int i=0; i < randomCount ; i++){
                arrayList.add(random.nextInt(randomNumber));
            }
            System.out.println("随机数集合:"+arrayList +" 长度:"+arrayList.size());
            Map<Integer, String> thradValueMap = Collections.synchronizedMap(new HashMap());
            Map<Integer, List<Integer>> mainValueMap = Collections.synchronizedMap(new HashMap());
            List countSizeListValue = new ArrayList(); //
            for(int i =0; i <threadNumber; i ++){
                int finalI = i;
                executors.execute(new Runnable() {
                    @Override
                    public void run(){
                        List<Integer> paramsList = new ArrayList(); //每个线程工作的内容
                        //思路：每个线程干10个值，10个线程干100个值
                        int startIndex;  //没个线程工作的10个值的尾坐标，也是没个线程管理数值的结束    //startIndex  endThreadIndex 因为for循环是反着的，所以这个值也是反着的
                        int endThreadIndex; //没个线程工作的10个值的头坐标，也是没个线程管理数值的开始
                        endThreadIndex = ((randomCount/threadNumber)*finalI);
                        if(finalI == threadNumber-1){
                            startIndex = ((randomCount - ((randomCount/threadNumber)*finalI)) + endThreadIndex) -1;
                        }else{
                            startIndex = endThreadIndex + ((randomCount/threadNumber)-1);
                        }
                        for(int  forStartIndex = startIndex; forStartIndex >= endThreadIndex; forStartIndex--){
                            paramsList.add(arrayList.get(forStartIndex)); //通过循环拿到每个线程需要工作的内容
                        }
                        //由于已知随机数最大的值是100,所以可以很容易的规范没个线程所管理的值范围，在这里暂定 1号线程工作范围为0-10.2号线程为11-20,以此类推
                        //start    此代码块规定此线程管理值的区间
                        int theManageValueMax;
                        int theManageValueMin;
                        if(finalI == threadNumber-1){//9
                            theManageValueMax = startIndex+1;
                            theManageValueMin = endThreadIndex;
                        }else{
                            theManageValueMax = startIndex;
                            theManageValueMin = endThreadIndex;
                        }
                        List<Integer> discardValue = new ArrayList<>(); //需要剔除的值的集合
                        for(int chickIndex = 0; chickIndex < workSpeace.get(finalI); chickIndex++){ //循环判断每个线程需要工作内容值
                            Integer workValue = paramsList.get(chickIndex); //每个线程工作的基本单位（数值）
                            if(theManageValueMin <=  workValue&&workValue <= theManageValueMax){
                                //如果符合工作数值区间，则什么都不做;
                                continue;
                            }else{ //如果不符合工作区间，则剔除到公共区域
                                Integer thisNumberParent = -1;
                                try {
                                    lock.lockInterruptibly();
                                    thisNumberParent = chickNumberParent(threadNumber, randomNumber, workValue); //调用此方法判断此随机数属于哪个线程的工作区间
                                }catch (Exception e){
                                    for(StackTraceElement itemError : e.getStackTrace()){
                                        System.out.println(itemError);
                                    }
                                }finally {
                                    lock.unlock();
                                }
                                String theValueByMap = thradValueMap.get(thisNumberParent);
                                if(theValueByMap == null || "".equals(theValueByMap)){
                                    thradValueMap.put(thisNumberParent,workValue.toString());
                                }else{
                                    thradValueMap.put(thisNumberParent,theValueByMap + ","+workValue.toString());
                                }
                                discardValue.add(workValue);
                            }
                        }
                        for(Integer itemDiscardvalue :discardValue){
                            paramsList.remove(new Integer(itemDiscardvalue.intValue()));
                        }
                        countDownLatch.countDown();
                        try {
                            countDownLatch.await();
                            String thisThreadWorkValue = thradValueMap.get(finalI); //当所有子线程工作完之后 从公共区域拿出本应该属于自己的值
                            if(thisThreadWorkValue == null || "".equals(thisThreadWorkValue)){ //如果公共区域没有属于自己的工作值

                            }else {
                                String thisThreadWorkValueSz[] = thisThreadWorkValue.split(",");
                                for(int i=0; i < thisThreadWorkValueSz.length; i++){
                                    paramsList.add(Integer.valueOf(thisThreadWorkValueSz[i]));
                                }
                                paramsList = sortInt(paramsList);
                            }
                            mainValueMap.put(finalI,paramsList);
                        }catch (Exception e){
                            e.fillInStackTrace();
                        }finally {
                            countDownLatchMain.countDown();
                        }
//                    System.out.println("分割线---------------------------------");
                    }
                });
            }
            countDownLatchMain.await();
            List<Integer> finalList = new ArrayList<>();
            for(int i =0; i<threadNumber; i++){
                List<Integer> itemList = mainValueMap.get(i);
                if(itemList == null || itemList.isEmpty()){
                    continue;
                }
                for(Integer itemValue : itemList){
                    finalList.add(itemValue);
                }
            }
            System.out.println("排序好的值:"+finalList+" 长度:"+finalList.size());
            arrayList.removeAll(finalList);
            System.out.println("差集为:"+arrayList);
        }catch (Exception e){
            for(StackTraceElement itemError :e.getStackTrace()){
                System.out.println(itemError);
            }
        }finally {
            executors.shutdownNow();
        }

    }


    /**
     *
     * @param paramsList 每个线程要干的事情...排序
     * @return
     */
    private static List<Integer> sortInt(List<Integer> paramsList){
        List<Integer> resutList = new LinkedList<>();
        for(int paramsListIndex = 0 ; paramsListIndex <paramsList.size(); paramsListIndex++){
            if(resutList.size() == 0){
                resutList.add(paramsList.get(paramsListIndex));
                continue;
            }
            for(int resultListIndex = resutList.size(); resultListIndex >= 0  ; resultListIndex--){
                if(resultListIndex == 0){
                    resutList.add(resultListIndex,paramsList.get(paramsListIndex));
                    break;
                }
                int paramsListInt = paramsList.get(paramsListIndex).intValue();
                int resultListInt = resutList.get(resultListIndex-1).intValue();
                if(paramsListInt <= resultListInt){
                    continue;
                }else{
                    resutList.add(resultListIndex,paramsList.get(paramsListIndex));
                    break;
                }
            }
        }

        return resutList;
    }

    /**
     * 判断该数值在map中的key应该是多少
     * @param threadNumber 线程量
     * @param randomNumber 随机数范围
     * @param needChickNumber 需要判断的值
     * @return
     */
    private static Integer chickNumberParent(Integer threadNumber,Integer randomNumber,Integer needChickNumber){
        Integer onlyThreadNumber = randomNumber/threadNumber; //单个线程负责值得区间， 比如 93个随机数  6个线程，  那么93/6 就是前五个的线程负责量，余数是最后一个线程负责量
        Integer valueIndex = needChickNumber / onlyThreadNumber;  //还以上一行注释为例子， 如果现在需要判断93这个值属于哪个线程的工作区间，则 93/（随机数数量/线程量）  93/15 = 6, 因为线程量一共为6,在list中长度最大值为5（因为list坐标从0开始），所以93这个值属于最后一个线程的工作区间
        if(valueIndex >= threadNumber){
            return valueIndex-1;
        }else{
            return valueIndex;
        }
    }


}
