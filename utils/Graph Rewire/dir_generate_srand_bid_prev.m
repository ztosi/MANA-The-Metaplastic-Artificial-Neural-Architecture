function dir_srand=dir_generate_srand_bid_prev(s1,ntry)
% Syntax: 
% dir_srand=dir_generate_srand(s1)
% s1 - the adjacency matrix of a directed network  
% ntry - (optional) the number of rewiring steps. If none is given ntry=4*(# of edges in the network)
% Output: dir_srand - the adjacency matrix of a randomized network with the same set of in- and out-degrees as the original one 

%tic;
dir_srand=s1;
nrew=0;
biOnly = (dir_srand ~= 0) & (dir_srand' ~= 0);

[i_srandBD, j_srandBD] = find(triu(biOnly));
[i_srandUD, j_srandUD] = find((dir_srand ~= 0) & ~biOnly);
i_srand = [i_srandBD; j_srandBD; i_srandUD];
j_srand = [j_srandBD; i_srandBD; j_srandUD];
bidtable = [(length(i_srandBD)+1):(2*length(i_srandBD))...
    1:length(i_srandBD)];

[Ne, ~]=size(i_srand);
if (nargin < 2)
    ntry=6*Ne;
end
inds = 1:Ne;
lpexecs = 0;

while lpexecs < ntry
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
   if (v1~=v3)&&(v1~=v4)&&(v2~=v4)&&(v2~=v3) ...
           && (dir_srand(v4,v1)==0)&&(dir_srand(v2,v3)==0)
       if (dir_srand(v1,v4)==0)&&(dir_srand(v3,v2)==0)
           if ((dir_srand(v2, v1) == 0) && (dir_srand(v4, v3) == 0))
                lpexecs = lpexecs + 1;
                dir_srand(v1,v4)=dir_srand(v1,v2);
                dir_srand(v3,v2)=dir_srand(v3,v4);            
                dir_srand(v1,v2)=0;
                dir_srand(v3,v4)=0;
                nrew=nrew+1;           
                j_srand(e1)=v4;
                j_srand(e2)=v2;                   
            elseif ((dir_srand(v2, v1) ~= 0) && (dir_srand(v4, v3) ~= 0))
%                 if (bidtable(e1)==0 || bidtable(e2) == 0)
%                    disp(dir_srand([v1 v2 v3 v4], [v1 v2 v3 v4])); 
%                    disp('prob');
%                 end
                lpexecs = lpexecs + 1;
                dir_srand(v3,v2)=dir_srand(v3,v4);
                dir_srand(v2,v3)=dir_srand(v2,v1);  
                dir_srand(v1,v4)=dir_srand(v1,v2);
                dir_srand(v4,v1)=dir_srand(v4,v3); 
                dir_srand(v3,v4)=0;
                dir_srand(v4,v3)=0;
                dir_srand(v1,v2)=0;
                dir_srand(v2,v1)=0;
                nrew=nrew+1;
%                 if (bidtable(e1)==0 || bidtable(e2) == 0)
%                    disp('prob'); 
%                 end

                e3 = bidtable(e1);
                e4 = bidtable(e2);

                j_srand(e1) = v4;
                j_srand(e2) = v2;
                j_srand(e3) = v3;
                j_srand(e4) = v1;
                
                bidtable(e1) = e4;
                bidtable(e2) = e3;
                bidtable(e3) = e2;
                bidtable(e4) = e1;
           end
       end
   end
end
%toc;
end

