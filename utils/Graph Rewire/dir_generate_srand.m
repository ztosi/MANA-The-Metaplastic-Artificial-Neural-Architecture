function dir_srand=dir_generate_srand(s1,ntry)
% Syntax: 
% dir_srand=dir_generate_srand(s1)
% s1 - the adjacency matrix of a directed network  
% ntry - (optional) the number of rewiring steps. If none is given ntry=4*(# of edges in the network)
% Output: dir_srand - the adjacency matrix of a randomized network with the same set of in- and out-degrees as the original one 
dir_srand=s1;
nrew=0;
[i_srand,j_srand]=find(dir_srand);
[Ne, ~]=size(i_srand);
if (nargin < 2)
    ntry=5*nnz(s1);
end
inds = 1:Ne;
for i=1:ntry;
   a = inds(1);
   j = randi(Ne);
   inds(1) = inds(j);
   inds(j) = a;
   a = inds(2);
   j = randi(Ne-1)+1;
   inds(2) = inds(j);
   inds(j) = a;
   e1 = inds(1);
   e2 = inds(2);
   v1=i_srand(e1);
   v2=j_srand(e1);
   v3=i_srand(e2);
   v4=j_srand(e2);
   if (v1~=v3)&&(v1~=v4)&&(v2~=v4)&&(v2~=v3);
       if (dir_srand(v1,v4)==0)&&(dir_srand(v3,v2)==0);
           if ((dir_srand(v2, v1) == 0) && (dir_srand(v4, v3) == 0))
                dir_srand(v1,v4)=dir_srand(v1,v2);
                dir_srand(v3,v2)=dir_srand(v3,v4);            
                dir_srand(v1,v2)=0;
                dir_srand(v3,v4)=0;
                nrew=nrew+1;           
                i_srand(e1)=v1;
                j_srand(e1)=v4;
                i_srand(e2)=v3;
                j_srand(e2)=v2;                
           end
           if ((dir_srand(v2, v1) ~= 0) && (dir_srand(v4, v3) ~= 0))
                dir_srand(v1,v4)=dir_srand(v1,v2);
                dir_srand(v3,v2)=dir_srand(v3,v4);            
                dir_srand(v1,v2)=0;
                dir_srand(v3,v4)=0;
                nrew=nrew+1;           
                i_srand(e1)=v1;
                j_srand(e1)=v4;
                i_srand(e2)=v3;
                j_srand(e2)=v2;                
           end
       end;
    end;
end;

