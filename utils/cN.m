function [cmmN, cmmNO, cmmNI] = cN (mat)
        mat = single(mat ~= 0);
        cmmNI = triu(mat' * mat, 1);
        cmmNO = triu(mat * mat', 1);
        cmmN = cmmNI + cmmNO;
end
