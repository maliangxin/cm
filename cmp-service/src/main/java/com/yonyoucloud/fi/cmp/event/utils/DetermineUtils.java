package com.yonyoucloud.fi.cmp.event.utils;


import com.yonyoucloud.fi.cmp.event.func.BranchHandleFunction;
import com.yonyoucloud.fi.cmp.event.func.ThrowExceptionFunction;
import lombok.extern.slf4j.Slf4j;
import com.yonyoucloud.fi.cmp.common.CtmException;

@Slf4j
public class DetermineUtils {

    private DetermineUtils() {
        throw new IllegalStateException("Utility class");
    }
    /**
     * <h2>去除if判断分支的代码，为false则抛出异常</h2>
     *
     * @param b :
     * @return com.yonYouCloud.fi.cmp.event.func.ThrowExceptionFunction
     * @author Sun GuoCai
     * @since 2023/6/4 21:12
     */
    public static ThrowExceptionFunction isTure(boolean b){
        return errorMessage -> {
            if (!b){
                log.error(errorMessage);
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100606"),errorMessage);
            }
        };
    }

    /**
     * 参数为true或false时，分别进行不同的操作
     *
     * @param b:
     * @return com.example.demo.func.BranchHandle
     **/
    public static BranchHandleFunction isTureOrFalse(boolean b){

        return (trueHandle, falseHandle) -> {
            if (b){
                trueHandle.run();
            } else {
                falseHandle.run();
            }
        };
    }
}
